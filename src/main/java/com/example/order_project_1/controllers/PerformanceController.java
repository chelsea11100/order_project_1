package com.example.order_project_1.controllers;

import com.example.order_project_1.models.entity.PerformanceRecords;
import com.example.order_project_1.services.PerformanceService;
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
import java.util.List;

@RestController
@RequestMapping("/api/performance")
public class PerformanceController {
    @Autowired
    private PerformanceService performanceService;

    @Value("${jwt.secret-key}")
    private String SECRET_KEY;

    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    // 获取 Token
    private String getTokenFromRequest(HttpServletRequest request) {
        String header = request.getHeader(TOKEN_HEADER);
        if (header != null && header.startsWith(TOKEN_PREFIX)) {
            return header;
        }
        return null;
    }

    // 验证用户角色
    private boolean getCurrentUserRole(HttpServletRequest request, String role) {
        String token = getTokenFromRequest(request);
        if (token != null) {
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

    // 获取工作人员绩效记录
    @GetMapping("/staff/{staffId}")
    public ResponseEntity<List<PerformanceRecords>> getPerformanceRecordsByStaffId(@PathVariable Long staffId, HttpServletRequest request) {
        // 这里原逻辑中 ADMIN 和 STAFF 角色同时满足的条件可能有误，推测应该是 ||
        if (getCurrentUserRole(request, "ADMIN") || getCurrentUserRole(request, "STAFF")) {
            return ResponseEntity.status(403).build(); // 403 表示禁止访问
        }
        List<PerformanceRecords> records = performanceService.getPerformanceRecordsByStaffId(staffId);
        return ResponseEntity.ok(records);
    }

    // 修改绩效记录
    @PutMapping("/{performanceId}")
    public ResponseEntity<PerformanceRecords> updatePerformance(@PathVariable Long performanceId, @RequestBody PerformanceRecords record, HttpServletRequest request) {
        if (getCurrentUserRole(request, "USER")) {
            return ResponseEntity.status(403).build(); // 403 表示禁止访问
        }
        // 获取 workload 并尝试转换为 Double 类型
        Double workload = Double.valueOf(record.getWorkload());
        if (workload == null) {
            // 若转换失败，可根据实际情况返回错误响应
            return ResponseEntity.badRequest().build();
        }
        PerformanceRecords updatedRecord = performanceService.updatePerformance(performanceId, workload);
        return ResponseEntity.ok(updatedRecord);
    }
}