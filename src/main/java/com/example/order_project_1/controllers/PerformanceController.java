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

import java.util.List;

@RestController
@RequestMapping("/api/performance")
public class PerformanceController {
    @Autowired
    private PerformanceService performanceService;

    @Autowired
    private OrderService orderService;


    @Value("${jwt.secret-key}")
    private String SECRET_KEY;

    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    // 获取 Token
    private String getTokenFromRequest(HttpServletRequest request) {
        String header = request.getHeader(TOKEN_HEADER);
        if (header != null && header.startsWith(TOKEN_PREFIX)) {
            return header.replace(TOKEN_PREFIX, "").trim(); // 移除前缀和空格
        }
        return null;
    }

    // 验证用户角色
    private boolean hasRole(HttpServletRequest request, String role) {
        String token = getTokenFromRequest(request);
        System.out.println(token);
        if (token != null) {
            try {
                Claims claims = Jwts.parser()
                        .setSigningKey(SECRET_KEY)
                        .parseClaimsJws(token.replace(TOKEN_PREFIX, ""))
                        .getBody();
                String userRole = claims.get("role", String.class);
                System.out.println("角色："+userRole);
                return role.equals(userRole);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    //从token拿出id
    private Long getUserIdFromToken(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        System.out.println("已成功获取到token："+token);
        if (token != null) {
            try {
                System.out.println("开始读取userid");
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

    // 工作人员获取自己绩效记录（可以了）
    @GetMapping("/staff/performance")
    public ResponseEntity<OrderHistoryResponse> getPerformanceRecordsByStaffId(HttpServletRequest request) {
        if (!hasRole(request, "STAFF")) {
            return ResponseEntity.status(403).build(); // 403 Forbidden
        }

        // 2. 从 Token 中提取 staffId
        Long staffId = getUserIdFromToken(request);
        System.out.println(staffId);
        if (staffId == null) {
            return ResponseEntity.status(401).build(); // 401 Unauthorized
        }

        // 3. 查询数据
        List<PerformanceRecords> records = performanceService.getPerformanceRecordsByStaffId(staffId);
        List<Orders> orders = orderService.getOrderHistory_1(staffId);
        OrderHistoryResponse response = new OrderHistoryResponse(orders, records);
        return ResponseEntity.ok(response);
    }

    //管理员获取所有绩效记录（这个可以了）
    @GetMapping("/performance")
    public ResponseEntity<OrderHistoryResponse> getAllPerformanceRecords(HttpServletRequest request) {
        // 1. 校验管理员权限
        if (!hasRole(request, "ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }



        // 2. 查询所有绩效记录
        try {
            Long staffId = getUserIdFromToken(request);
            List<PerformanceRecords> records = performanceService.getAllPerformanceRecords();
            List<Orders> orders = orderService.getOrderHistory_1(staffId);
            if(records!=null) {
                System.out.println("records以获取");
            }
            if(orders!=null) {
                System.out.println("orders已获取");
            }
            OrderHistoryResponse response = new OrderHistoryResponse(orders, records);
            if(response!=null) {
                System.out.println("response以获取");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    //管理员获取一个工作人员绩效记录（这个可以了）
    @GetMapping("/performance/{staffId}")
    public ResponseEntity<OrderHistoryResponse> getStaffPerformanceRecordsByAdmin(
            HttpServletRequest request,
            @PathVariable Long staffId) {
        if (!hasRole(request, "ADMIN")) {
            return ResponseEntity.status(403).build(); // 403 Forbidden
        }
        List<PerformanceRecords> records = performanceService.getPerformanceRecordsByStaffId(staffId);
        List<Orders> orders = orderService.getOrderHistory_1(staffId);
        OrderHistoryResponse response = new OrderHistoryResponse(orders, records);
        return ResponseEntity.ok(response);
    }

    // 修改绩效记录（可以了）
    @PutMapping("/{performanceId}")
    public ResponseEntity<PerformanceRecords> updatePerformance(@PathVariable Long performanceId, @RequestBody PerformanceRecords record, HttpServletRequest request) {
        if (hasRole(request, "USER")) {
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