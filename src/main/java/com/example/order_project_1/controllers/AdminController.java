package com.example.order_project_1.controllers;

import com.example.order_project_1.models.entity.Orders;
import com.example.order_project_1.models.entity.Users;
import com.example.order_project_1.services.OrderService;
import com.example.order_project_1.services.UserService;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    // 登录
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Users user) {
        Optional<Users> loggedInUser = userService.loginUser(user.getUsername(), user.getPassword());
        if (loggedInUser.isPresent()) {
            // 登录成功，将用户信息存入会话
            HttpSession session = ((HttpServletRequest) ServletRequest.class.cast(null)).getSession(true);
            session.setAttribute("user", loggedInUser.get());
            return ResponseEntity.ok(loggedInUser.get());
        } else {
            // 登录失败，返回错误信息
            return ResponseEntity.status(401).body("Invalid username or password");
        }
    }

    // 检查用户是否具有指定角色
    private boolean hasRole(HttpServletRequest request, String role) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Users user = (Users) session.getAttribute("user");
            if (user != null) {
                String userRole = user.getRole();
                System.out.println("用户角色：" + userRole);

                System.out.println(userRole);
                if (userRole != null) {
                    System.out.println(2);
                    return userRole.equals(role);
                }
            }
        }
        return false;
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

    @PutMapping("/staff/{staffId}/permissions")
    public ResponseEntity<Users> updateStaffPermissions(@PathVariable Long staffId, @RequestBody Users user, HttpServletRequest request) {
        if (hasRole(request, "ADMIN")) {
            return ResponseEntity.ok(userService.updateStaffPermissions(staffId, user));
        } else {
            return ResponseEntity.status(403).build();
        }
    }

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

    // 管理员和工作人员查看所有未接订单
    @GetMapping("/orders/unassigned")
    public ResponseEntity<List<Orders>> listUnassignedOrders(HttpServletRequest request) {
        if (hasRole(request, "ADMIN") || hasRole(request, "STAFF")) {
            List<Orders> orders = userService.findUnassignedOrders();
            return ResponseEntity.ok(orders);
        } else {
            return ResponseEntity.status(403).build();
        }
    }

    // 手动派单给工作人员
    @PostMapping("/orders/{orderId}/assign")
    public ResponseEntity<Orders> assignOrderToStaff(@PathVariable Long orderId, @RequestBody Long staffId, HttpServletRequest request) {
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
            HttpSession session = request.getSession(false);
            if (session != null) {
                Users user = (Users) session.getAttribute("user");
                if (user != null) {
                    Users adminProfile = userService.getAdminProfile(user.getId());
                    if (adminProfile != null) {
                        return ResponseEntity.ok(adminProfile);
                    }
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
        if (hasRole(request, "ADMIN")) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Users loggedInUser = (Users) session.getAttribute("user");
                if (loggedInUser != null) {
                    Users updatedAdmin = userService.updateAdminProfile(loggedInUser.getId(), user);
                    if (updatedAdmin != null) {
                        return ResponseEntity.ok(updatedAdmin);
                    }
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
            HttpSession session = request.getSession(false);
            if (session != null) {
                Users user = (Users) session.getAttribute("user");
                if (user != null) {
                    Users staffProfile = userService.getStaffProfile(user.getId());
                    if (staffProfile != null) {
                        return ResponseEntity.ok(staffProfile);
                    }
                }
            }
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.status(403).build();
        }
    }

    // 工作人员修改个人信息
    @PutMapping("/staff/profile")
    public ResponseEntity<Users> updateStaffProfile(@RequestBody Users user, HttpServletRequest request) {
        if (hasRole(request, "STAFF")) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Users loggedInUser = (Users) session.getAttribute("user");
                if (loggedInUser != null) {
                    Users updatedStaff = userService.updateStaffProfile(loggedInUser.getId(), user);
                    if (updatedStaff != null) {
                        return ResponseEntity.ok(updatedStaff);
                    }
                }
            }
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.status(403).build();
        }
    }

    // 登出
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }
}