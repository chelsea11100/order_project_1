package com.example.order_project_1.controllers;

import com.example.order_project_1.models.entity.Users;
import org.springframework.http.HttpStatus;
import com.example.order_project_1.models.entity.Orders;
import com.example.order_project_1.services.OrderService;
import com.example.order_project_1.services.PerformanceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Enumeration;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    @Autowired
    private OrderService orderService;
    @Autowired
    private PerformanceService performanceRecordService;

    // 手动验证用户角色的方法
    private boolean hasRole(HttpServletRequest request, String role) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Users user = (Users) session.getAttribute("user");
            if (user != null) {
                String userRole = user.getRole();
                System.out.println("用户角色：" + userRole);

                System.out.println(userRole);
                if (userRole != null) {
                    return userRole.equals(role);
                }
            }
        }
        return false;
    }

    // 用户创建订单
    @PostMapping
    public ResponseEntity<Orders> createOrder(@RequestBody Orders order, HttpServletRequest request) {
        // 输入验证
        if (order == null) {
            logger.error("创建订单时，传入的订单对象为空");
            return ResponseEntity.badRequest().build();
        }
        // 权限验证
        if (!hasRole(request, "USER")) {
            System.out.println(order.getUserId());
            logger.warn("用户没有创建订单的权限，请求被拒绝");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Orders());
        }
        try {
            // 创建订单
            Orders createdOrder = orderService.createOrder(order);
            logger.info("订单创建成功，订单 ID: {}", createdOrder.getId()); // 假设 Orders 类有 getId 方法
            return ResponseEntity.ok(createdOrder);
        } catch (Exception e) {
            // 异常处理
            logger.error("创建订单时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new Orders());
        }
    }

    // 用户填写订单反馈（涉及result和userfeedback）
    @PostMapping("/{orderId}/feedback")
    public ResponseEntity<Void> submitOrderFeedback(@PathVariable Long orderId,
                                                    @RequestBody String feedback,
                                                    @RequestBody String result,
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
            if (order != null) {
                return ResponseEntity.ok(order);
            } else {
                return ResponseEntity.notFound().build();
            }
        } else {
            return ResponseEntity.status(403).build();
        }
    }

    // 取消订单
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Boolean> cancelOrder(@PathVariable Long orderId, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            // 假设登录时将用户 ID 和角色信息存入会话
            Long userId = (Long) session.getAttribute("userId");
            String userRole = (String) session.getAttribute("role");
            if ((userRole.equals("USER") || userRole.equals("ADMIN")) && userId != null) {
                boolean success = orderService.cancelOrder(orderId, userId, userRole);
                return ResponseEntity.ok(success);
            }
        }
        return ResponseEntity.status(403).build();
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

    // 交给ai通过feedback设定工作量（评分）
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

    // ai自动派单（派单逻辑还没写）
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
