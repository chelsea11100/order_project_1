package com.example.order_project_1.controllers;

import com.example.order_project_1.models.entity.Users;
import com.example.order_project_1.services.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    @Value("${jwt.secret-key}")
    private String SECRET_KEY;

    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 10; // 10个小时（试一下）


    private boolean hasRole(HttpServletRequest request, String role) {
        String token = getTokenFromRequest(request);
        if (token != null) {
            System.out.println("已经获取到token在hasrole方法里");
            try {
                Claims claims = Jwts.parser()
                        .setSigningKey(SECRET_KEY)
                        .parseClaimsJws(token.replace(TOKEN_PREFIX, ""))
                        .getBody();
                String userRole = claims.get("role", String.class);
                return role.equals(userRole);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    // 获取 Token
    private String getTokenFromRequest(HttpServletRequest request) {
        String header = request.getHeader(TOKEN_HEADER);
        if (header != null && header.startsWith(TOKEN_PREFIX)) {
            return header;
        }
        return null;
    }

    // 生成 Token
    private String generateToken(Users user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole());
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    @PostMapping("/register")
    public ResponseEntity<Users> registerUser(@RequestBody Users user, HttpServletRequest request) {
        Users registeredUser = userService.registerUser(user);
        System.out.println(user.getStudentIdOrEmployeeId());
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> loginUser(@RequestBody Users user, HttpServletRequest request) {
        Optional<Users> optionalUser = userService.loginUser(user.getUsername(), user.getPassword());
        if (optionalUser.isPresent()) {
            Users loggedInUser = optionalUser.get();
            String token = generateToken(loggedInUser);
            Map<String, Object> response = new HashMap<>();
            response.put("user", loggedInUser);
            response.put("token", token);
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(401).build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logoutUser(HttpServletRequest request) {
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/profile")
    public ResponseEntity<Users> updateProfile(@RequestBody Users user, HttpServletRequest request) {
        System.out.println(hasRole(request, "USER"));
        if (hasRole(request, "USER")) {
            String token = getTokenFromRequest(request);
            if (token != null) {
                try {
                    Claims claims = Jwts.parser()
                            .setSigningKey(SECRET_KEY)
                            .parseClaimsJws(token.replace(TOKEN_PREFIX, ""))
                            .getBody();
                    String username = claims.getSubject();
                    Optional<Users> optionalUser = userService.findUserByUsername(username);
                    if (optionalUser.isPresent()) {
                        Users loggedInUser = optionalUser.get();
                        Users updatedUser = userService.updateUserProfile(loggedInUser.getId(), user);
                        return ResponseEntity.ok(updatedUser);
                    }
                } catch (Exception e) {
                    return ResponseEntity.status(403).build();
                }
            }
        }
        return ResponseEntity.status(403).build();
    }

    @GetMapping("/profile")
    public ResponseEntity<Users> getProfile(HttpServletRequest request) {
        System.out.println(hasRole(request, "USER"));
        if (hasRole(request, "USER")) {
            System.out.println("用户验证成功");
            String token = getTokenFromRequest(request);
            if (token != null) {
                System.out.println("token获取成功");
                try {
                    Claims claims = Jwts.parser()
                            .setSigningKey(SECRET_KEY)
                            .parseClaimsJws(token.replace(TOKEN_PREFIX, ""))
                            .getBody();
                    String username = claims.getSubject();
                    Optional<Users> optionalUser = userService.findUserByUsername(username);
                    if (optionalUser.isPresent()) {
                        Users loggedInUser = optionalUser.get();
                        Users userProfile = userService.getUserProfile(loggedInUser.getId());
                        return ResponseEntity.ok(userProfile);
                    }
                } catch (Exception e) {
                    return ResponseEntity.status(403).build();
                }
            }
        }
        return ResponseEntity.status(403).build();
    }
}