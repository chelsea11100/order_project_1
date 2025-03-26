package com.example.order_project_1.controllers;

import com.example.order_project_1.DTO.OrderHistoryResponse;
import com.example.order_project_1.models.entity.Orders;
import com.example.order_project_1.models.entity.PerformanceRecords;
import com.example.order_project_1.services.OrderService;
import com.example.order_project_1.services.PerformanceService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);//试一下（引入日志）

    @Value("${jwt.secret-key}")
    private String SECRET_KEY;

    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    @Autowired
    private OrderService orderService;
    @Autowired
    private PerformanceService performanceRecordService;

    private String getTokenFromRequest(HttpServletRequest request) {
        String header = request.getHeader(TOKEN_HEADER);

        return header != null && header.startsWith(TOKEN_PREFIX) ?
                header.replace(TOKEN_PREFIX, "") : null;
    }

    private boolean hasRole(HttpServletRequest request, String role) {
        String token = getTokenFromRequest(request);
        System.out.println("token"+token);
        if (token == null) return false;

        try {
            Claims claims = Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
            return role.equals(claims.get("role", String.class));
        } catch (Exception e) {
            logger.error("Token 解析失败: {}", e.getMessage());
            return false;
        }
    }

    private Long getUserIdFromToken(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (token == null) return null;

        try {
            Claims claims = Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
            return claims.get("userId", Long.class); // 假设 Token 中存储了 userId
        } catch (Exception e) {
            logger.error("获取用户 ID 失败: {}", e.getMessage());
            return null;
        }
    }


    // 用户创建订单
    @PostMapping("/creatDetail")
    public ResponseEntity<Orders> createOrder(@RequestBody Orders order, HttpServletRequest request) {
        // 检查订单对象是否为空
        if (order == null) {
            logger.error("创建订单时，传入的订单对象为空");
            return ResponseEntity.badRequest().build();
        }

        // 检查用户是否有权限创建订单
        if (!hasRole(request, "USER")) {
            logger.warn("用户没有创建订单的权限，请求被拒绝");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Orders());
        }

        // 从 Token 中获取当前用户的 user_id
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            logger.error("无法从 Token 中获取用户 ID");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Orders());
        }

        // 设置 user_id
        order.setUserId(userId);

        try {
            // 调用服务层创建订单
            Orders createdOrder = orderService.createOrder(order);
            logger.info("订单创建成功，订单 ID: {}", createdOrder.getId());
            return ResponseEntity.ok(createdOrder);
        } catch (Exception e) {
            logger.error("创建订单时发生未知异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new Orders());
        }
    }


    @PostMapping("/{orderId}/feedback")
    public ResponseEntity<Void> submitOrderFeedback(
            @PathVariable Long orderId,
            @RequestParam String feedback,
            @RequestParam String result,
            HttpServletRequest request) {

        // 检查用户是否有权限提交反馈
        if (!hasRole(request, "USER")) {
            logger.warn("用户没有提交反馈的权限，请求被拒绝");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 从 Token 中获取当前用户的 user_id
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            logger.error("无法从 Token 中获取用户 ID");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // 调用服务层提交反馈
            orderService.submitOrderFeedback(orderId, feedback, result, userId);
            Orders order = orderService.getOrderDetails(orderId);
            if (order != null) {
                performanceRecordService.evaluateWorkload(order);
            }
            logger.info("用户 {} 成功为订单 {} 提交反馈", userId, orderId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.error("提交反馈时发生参数错误: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("提交反馈时发生未知异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    // 管理人员查看所有订单时的搜索框 1.用户id 2.工作人员id
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Orders>> getOrdersByUserId(@PathVariable Long userId, HttpServletRequest request) {
        if (hasRole(request, "ADMIN")) {
            List<Orders> orders = orderService.getOrdersByUserId(userId);
            return ResponseEntity.ok(orders);
        } else {
            return ResponseEntity.status(403).build();
        }
    }


    @GetMapping("/staff/{staffId}")
    public ResponseEntity<List<Orders>> getOrdersByStaffId(@PathVariable Long staffId, HttpServletRequest request) {
        if (hasRole(request, "ADMIN")) {
            List<Orders> orders = orderService.getOrdersByStaffId(staffId);
            return ResponseEntity.ok(orders);
        } else {
            return ResponseEntity.status(403).build();
        }
    }


    // 管理员查看所有订单
    @GetMapping("/all")
    public ResponseEntity<OrderHistoryResponse> getAllOrders() {
        List<Orders> orders=orderService.getAllOrders();
        List<PerformanceRecords> performanceRecords = performanceRecordService.getAllPerformanceRecords();
        OrderHistoryResponse response = new OrderHistoryResponse(orders, performanceRecords);
        return ResponseEntity.ok(response);
    }


    // 查询历史订单
    @GetMapping("/user/history")
    public ResponseEntity<OrderHistoryResponse> getOrderHistory(HttpServletRequest request) {
        if (hasRole(request, "USER") || hasRole(request, "STAFF")) {
            Long userId = getUserIdFromToken(request);

            // 根据角色获取订单数据
            List<Orders> orders;
            if (hasRole(request, "USER")) {
                orders = orderService.getOrderHistory(userId);
                System.out.println("已获取到了orders");
            } else {
                orders = orderService.getOrderHistory_1(userId); // <-- 注意检查方法名是否需要统一

            }

            // 获取绩效记录
            List<PerformanceRecords> performanceRecords = performanceRecordService.getPerformanceRecordsByStaffId(userId);
            if(performanceRecords!=null) {
                System.out.println("已获取到了performanceRecords");
            }

            // 组合响应对象
            OrderHistoryResponse response = new OrderHistoryResponse(orders, performanceRecords);
            if(response!=null) {
                System.out.println("已获取到response");
            }
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(403).build();
        }
    }


    // 查看订单详情
    @GetMapping("/{orderId}")
    public ResponseEntity<Orders> getOrderDetails(@PathVariable Long orderId, HttpServletRequest request) {
        if ( hasRole(request, "STAFF") || hasRole(request, "ADMIN")) {
            Orders order = orderService.getOrderDetails(orderId);
            return order != null ?
                    ResponseEntity.ok(order) :
                    ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.status(403).build();
        }
    }


    // 取消订单
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Boolean> cancelOrder(@PathVariable Long orderId, HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        String userRole = getRoleFromToken(request); // 新增辅助方法

        if (userId == null || userRole == null) {
            return ResponseEntity.status(403).build();
        }

        if (("USER".equals(userRole) || "ADMIN".equals(userRole))) {
            boolean success = orderService.cancelOrder(orderId, userId, userRole);
            return ResponseEntity.ok(success);
        }
        return ResponseEntity.status(403).build();
    }

    // 辅助方法：从 Token 中获取角色
    private String getRoleFromToken(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (token == null) return null;
        try {
            Claims claims = Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
            return claims.get("role", String.class);
        } catch (Exception e) {
            return null;
        }
    }


    // 工作人员更新订单状态
    @PutMapping("/{orderId}/status")
    public ResponseEntity<Orders> updateOrderStatus(@PathVariable Long orderId, HttpServletRequest request) {
        if (hasRole(request, "STAFF")) {
            Orders updatedOrder = orderService.updateOrderStatus(orderId,"已完成");
            return ResponseEntity.ok(updatedOrder);
        } else {
            return ResponseEntity.status(403).build();
        }
    }



}