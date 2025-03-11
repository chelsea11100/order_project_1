package com.example.order_project_1.controllers;

import com.example.order_project_1.models.entity.Users;
import com.example.order_project_1.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    // 手动验证用户角色的方法
    private boolean hasRole(HttpServletRequest request, String role) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Users user = (Users) session.getAttribute("user");
            if (user != null) {
                return role.equals(user.getRole());
            }
        }
        return false;
    }

    @PostMapping("/register")
    public ResponseEntity<Users> registerUser(@RequestBody Users user, HttpServletRequest request) {
        // 注册不需要角色验证
        Users registeredUser = userService.registerUser(user);
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<Users> loginUser(@RequestBody Users user, HttpServletRequest request) {
        Optional<Users> optionalUser = userService.loginUser(user.getUsername(), user.getPassword());
        if (optionalUser.isPresent()) {
            Users loggedInUser = optionalUser.get();
            HttpSession session = request.getSession(true);
            session.setAttribute("user", loggedInUser);
            return ResponseEntity.ok(loggedInUser);
        }
        return ResponseEntity.status(401).build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logoutUser(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/profile")
    public ResponseEntity<Users> updateProfile(@RequestBody Users user, HttpServletRequest request) {
        if (hasRole(request, "USER")) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Users loggedInUser = (Users) session.getAttribute("user");
                if (loggedInUser != null) {
                    Users updatedUser = userService.updateUserProfile(loggedInUser.getId(), user);
                    return ResponseEntity.ok(updatedUser);
                }
            }
        }
        return ResponseEntity.status(403).build();
    }

    @GetMapping("/profile")
    public ResponseEntity<Users> getProfile(HttpServletRequest request) {
        if (hasRole(request, "USER")) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Users loggedInUser = (Users) session.getAttribute("user");
                if (loggedInUser != null) {
                    Users userProfile = userService.getUserProfile(loggedInUser.getId());
                    return ResponseEntity.ok(userProfile);
                }
            }
        }
        return ResponseEntity.status(403).build();
    }
}