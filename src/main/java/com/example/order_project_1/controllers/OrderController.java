package com.example.order_project_1.controllers;

import com.example.order_project_1.models.entity.Orders;
import com.example.order_project_1.services.OrderService;
import com.example.order_project_1.services.PerformanceService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);//试一下（引入日志）

    private static final SecretKey SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
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
    @PostMapping
    public ResponseEntity<Orders> createOrder(@RequestBody Orders order, HttpServletRequest request) {
        if (order == null) {
            logger.error("创建订单时，传入的订单对象为空");
            return ResponseEntity.badRequest().build();
        }
        if (!hasRole(request, "USER")) {
            logger.warn("用户没有创建订单的权限，请求被拒绝");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Orders());
        }
        try {
            Orders createdOrder = orderService.createOrder(order);
            logger.info("订单创建成功，订单 ID: {}", createdOrder.getId());
            return ResponseEntity.ok(createdOrder);
        } catch (Exception e) {
            logger.error("创建订单时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new Orders());
        }
    }


    // 用户填写订单反馈（涉及result和userfeedback）
    @PostMapping("/{orderId}/feedback")
    public ResponseEntity<Void> submitOrderFeedback(
            @PathVariable Long orderId,
            @RequestParam String feedback,
            @RequestParam String result,
            HttpServletRequest request) {

        if (hasRole(request, "USER")) {
            orderService.submitOrderFeedback(orderId, feedback, result);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(403).build();
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
    public List<Orders> getAllOrders() {
        return orderService.getAllOrders();
    }


    // 查询历史订单
    @GetMapping("/user/{userId}/history")
    public ResponseEntity<List<Orders>> getOrderHistory(@PathVariable Long userId, HttpServletRequest request) {
        if (hasRole(request, "USER") || hasRole(request, "STAFF")) {
            List<Orders> orders = orderService.getOrderHistory(userId);
            return ResponseEntity.ok(orders);
        } else {
            return ResponseEntity.status(403).build();
        }
    }


    // 查看订单详情
    @GetMapping("/{orderId}")
    public ResponseEntity<Orders> getOrderDetails(@PathVariable Long orderId, HttpServletRequest request) {
        if (hasRole(request, "USER") || hasRole(request, "STAFF") || hasRole(request, "ADMIN")) {
            Orders order = orderService.getOrderDetails(orderId);
            return order != null ?
                    ResponseEntity.ok(order) :
                    ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.status(403).build();
        }
    }


    // 取消订单（重点改造部分）
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
    public ResponseEntity<Orders> updateOrderStatus(@PathVariable Long orderId, @RequestParam String status, HttpServletRequest request) {
        if (hasRole(request, "STAFF")) {
            Orders updatedOrder = orderService.updateOrderStatus(orderId, status);
            return ResponseEntity.ok(updatedOrder);
        } else {
            return ResponseEntity.status(403).build();
        }
    }


    // 交给AI通过feedback设定工作量（评分）
    @PostMapping("/{orderId}/feedbacks")
    public ResponseEntity<Void> submitFeedback(@PathVariable Long orderId, HttpServletRequest request) {
        if (hasRole(request, "USER") || hasRole(request, "STAFF") || hasRole(request, "ADMIN")) {
            Orders order = orderService.getOrderDetails(orderId);
            if (order != null) {
                performanceRecordService.evaluateWorkload(order);
            }
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(403).build();
        }
    }


    // AI自动派单
    @PostMapping("/ai-assign")
    public ResponseEntity<Void> autoAssignOrder(@RequestBody Orders order, HttpServletRequest request) {
        if (hasRole(request, "ADMIN") || hasRole(request, "STAFF")) {
            orderService.autoAssignOrder(order);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(403).build();
        }
    }
}