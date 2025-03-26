package com.example.order_project_1.controllers;

import com.example.order_project_1.models.entity.Orders;
import com.example.order_project_1.models.entity.Users;
import com.example.order_project_1.services.OrderService;
import com.example.order_project_1.services.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    @Value("${jwt.secret-key}")
    private String SECRET_KEY;

    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    // 从请求中获取Token
    private String extractToken(HttpServletRequest request) {
        // 1. 获取请求头
        String header = request.getHeader(TOKEN_HEADER);

        // 2. 添加调试日志（关键！）
        System.out.println("[DEBUG] Authorization Header: " + header);

        // 3. 验证并提取 Token
        if (header != null && header.startsWith(TOKEN_PREFIX)) {
            return header.replace(TOKEN_PREFIX, "").trim(); // 清理多余空格
        }

        return null;
    }

    // 检查用户是否具有指定角色
    private boolean hasRole(HttpServletRequest request, String role) {
        String token = extractToken(request);
        System.out.println("token是： "+token);
        if (token != null) {
            try {
                Claims claims = Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
                String userRole = claims.get("role", String.class);
                return role.equals(userRole);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    // 从Token中获取用户ID
    private Long getUserIdFromToken(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            try {
                Claims claims = Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
                Long userId = claims.get("userId", Long.class);
                System.out.println("[DEBUG] Parsed userId: " + userId);
                return claims.get("userId", Long.class);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    // 管理员管理工作人员的四个方法
    @PostMapping("/staff")
    public ResponseEntity<Users> addStaff(@RequestBody Users user, HttpServletRequest request) {
        if (hasRole(request, "ADMIN")) {
            return ResponseEntity.ok(userService.addStaff(user));
        } else {
            return ResponseEntity.status(403).build();
        }
    }

    @DeleteMapping("/staff/{staffId}")
    public ResponseEntity<Void> deleteStaff(@PathVariable Long staffId, HttpServletRequest request) {
        if (hasRole(request, "ADMIN")) {
            userService.deleteStaff(staffId);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(403).build();
        }
    }



    //搜索框根据员工id查找员工
    @GetMapping("/staff/{staffId}")
    public ResponseEntity<Users> getStaff(@PathVariable Long staffId, HttpServletRequest request) {
        if (hasRole(request, "ADMIN")) {
            Users staff = userService.getStaff(staffId);
            if (staff != null) {
                return ResponseEntity.ok(staff);
            } else {
                return ResponseEntity.notFound().build();
            }
        } else {
            return ResponseEntity.status(403).build();
        }
    }

    //管理员查看所有员工信息的方法
    @GetMapping("/staff/getStaffs")
    public ResponseEntity<List<Users>> getStaffs(HttpServletRequest request) {
        if(hasRole(request,"ADMIN")) {
            List<Users> users = userService.getStaffs();
            return  ResponseEntity.ok(users);
        }
        else {
            return ResponseEntity.status(403).build();
        }
    }



    // 管理员和工作人员查看所有未接订单
    @GetMapping("/orders/unassigned")
    public ResponseEntity<List<Orders>> listUnassignedOrders(HttpServletRequest request) {
        if (hasRole(request, "ADMIN") || hasRole(request, "STAFF")) {
            List<Orders> orders = userService.findUnassignedOrders(); // 注意这里原代码userService应该是orderService
            return ResponseEntity.ok(orders);
        } else {
            return ResponseEntity.status(403).build();
        }
    }

    // 手动派单给工作人员
    @PostMapping("/orders/{orderId}/assign")
    public ResponseEntity<Orders> assignOrderToStaff(@PathVariable Long orderId, @RequestParam Long staffId, HttpServletRequest request) {
        if (hasRole(request, "ADMIN")) {
            Orders updatedOrder = userService.assignOrderToStaff(orderId, staffId);
            if (updatedOrder != null) {
                return ResponseEntity.ok(updatedOrder);
            } else {
                return ResponseEntity.notFound().build();
            }
        } else {
            return ResponseEntity.status(403).build();
        }
    }

    // 展示管理员个人信息
    @GetMapping("/profile")
    public ResponseEntity<Users> getAdminProfile(HttpServletRequest request) {
        if (hasRole(request, "ADMIN")) {
            Long userId = getUserIdFromToken(request);
            if (userId != null) {
                Users adminProfile = userService.getAdminProfile(userId);
                if (adminProfile != null) {
                    return ResponseEntity.ok(adminProfile);
                }
            }
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.status(403).build();
        }
    }

    // 管理员修改个人信息
    @PutMapping("/profile")
    public ResponseEntity<Users> updateAdminProfile(@RequestBody Users user, HttpServletRequest request) {
        if (hasRole(request, "ADMIN")||hasRole(request, "STAFF")) {
            Long userId = getUserIdFromToken(request);
            if (userId != null) {
                Users updatedAdmin = userService.updateAdminProfile(userId, user);
                if (updatedAdmin != null) {
                    return ResponseEntity.ok(updatedAdmin);
                }
            }
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.status(403).build();
        }
    }

    // 工作人员查看个人信息
    @GetMapping("/staff/profile")
    public ResponseEntity<Users> getStaffProfile(HttpServletRequest request) {
        if (hasRole(request, "STAFF")) {
            Long userId = getUserIdFromToken(request);
            if (userId != null) {
                Users staffProfile = userService.getStaffProfile(userId);
                if (staffProfile != null) {
                    return ResponseEntity.ok(staffProfile);
                }
            }
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.status(403).build();
        }
    }



    // 登出（Token无状态，前端直接清除Token即可，后端无需特殊处理）
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.noContent().build();
    }
}