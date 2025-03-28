package com.example.order_project_1.controllers;
import com.example.order_project_1.models.entity.Appeals;
import com.example.order_project_1.services.AppealsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
@RestController
@RequestMapping("/api/appeals")
public class AppealsController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    @Autowired
    private AppealsService appealsService;

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

    // **工作人员提交申诉**
    @PostMapping("/create")
    public ResponseEntity<Appeals> createAppeal(
            HttpServletRequest request,
            @RequestParam Long performanceRecordId,
            @RequestParam String reason) {

        // 从 Token 中获取当前用户的 user_id
        Long currentUserId = getUserIdFromToken(request);
        if (currentUserId == null) {
            logger.warn("无法从 Token 中获取用户 ID");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 权限验证
        if (!hasRole(request, "STAFF")) {
            logger.warn("用户没有权限提交申诉，请求被拒绝");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            // 调用服务层创建申诉
            Appeals appeal = appealsService.createAppeal(currentUserId, performanceRecordId, reason);
            logger.info("工作人员 {} 成功提交申诉，申诉 ID: {}", currentUserId, appeal.getId());
            return ResponseEntity.ok(appeal);
        } catch (IllegalArgumentException e) {
            logger.error("提交申诉时发生参数错误: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("提交申诉时发生未知异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    // **管理员审批申诉**
    @PostMapping("/review/{appealId}")
    public ResponseEntity<Appeals> reviewAppeal(
            HttpServletRequest request,
            @PathVariable Long appealId,
            @RequestParam boolean approve) {

        // 权限验证
        if (!hasRole(request, "ADMIN")) {
            logger.warn("用户没有权限审批申诉，请求被拒绝");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            // 调用服务层审批申诉
            Appeals appeal = appealsService.reviewAppeal(appealId, approve);
            logger.info("管理员成功审批申诉，申诉 ID: {}", appealId);
            return ResponseEntity.ok(appeal);
        } catch (IllegalArgumentException e) {
            logger.error("审批申诉时发生参数错误: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("审批申诉时发生未知异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // **管理员获取所有申诉**
    @GetMapping("/all")
    public ResponseEntity<List<Appeals>> getAllAppeals(HttpServletRequest request) {
        // 权限验证
        if (!hasRole(request, "ADMIN")) {
            logger.warn("用户没有权限获取所有申诉，请求被拒绝");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            // 调用服务层获取所有申诉
            List<Appeals> appeals = appealsService.getAllAppeals();
            logger.info("管理员成功获取所有申诉，数量: {}", appeals.size());
            return ResponseEntity.ok(appeals);
        } catch (Exception e) {
            logger.error("获取所有申诉时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // **管理员根据工作人员 ID 获取申诉**
    @GetMapping("/staff/{staffId}")
    public ResponseEntity<List<Appeals>> getAppealsByStaffId(
            HttpServletRequest request,
            @PathVariable Long staffId) {

        // 权限验证
        if (!hasRole(request, "ADMIN")) {
            logger.warn("用户没有权限获取工作人员的申诉，请求被拒绝");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            // 调用服务层获取工作人员的申诉
            List<Appeals> appeals = appealsService.getAppealsByStaffId(staffId);
            logger.info("管理员成功获取工作人员 {} 的申诉，数量: {}", staffId, appeals.size());
            return ResponseEntity.ok(appeals);
        } catch (Exception e) {
            logger.error("获取工作人员的申诉时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
