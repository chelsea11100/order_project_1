package com.example.order_project_1.controllers;
import com.example.order_project_1.DTO.AppealWithOrderDTO;
import com.example.order_project_1.models.entity.Appeals;
import com.example.order_project_1.models.entity.Orders;
import com.example.order_project_1.models.entity.PerformanceRecords;
import com.example.order_project_1.services.AppealsService;
import com.example.order_project_1.services.OrderService;
import com.example.order_project_1.services.PerformanceService;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
@RestController
@RequestMapping("/api/appeals")
public class AppealsController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    @Autowired
    private AppealsService appealService;
    @Autowired
    private PerformanceService performanceService;

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
            Appeals appeal = appealService.createAppeal(currentUserId, performanceRecordId, reason);
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
            Appeals appeal = appealService.reviewAppeal(appealId, approve);
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
    // **管理员获取所有申诉**
    @GetMapping("/all")
    public ResponseEntity<List<AppealWithOrderDTO>> getAllAppealsWithOrders(HttpServletRequest request) {
        // 1. 权限校验
        if (!hasRole(request, "ADMIN")) {
            logger.warn("权限不足");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            // 2. 获取所有申诉记录
            List<Appeals> appeals = appealService.getAllAppeals();

            // 3. 批量获取关联数据（性能优化）
            List<Long> performanceIds = appeals.stream()
                    .map(Appeals::getPerformanceRecordId)
                    .collect(Collectors.toList());

            // 获取绩效记录Map (performance_id -> PerformanceRecords)
            Map<Long, PerformanceRecords> performanceMap = performanceService.getPerformanceRecordsDetails(performanceIds)
                    .stream()
                    .collect(Collectors.toMap(PerformanceRecords::getId, Function.identity()));

            // 获取订单ID列表
            List<Long> orderIds = performanceMap.values().stream()
                    .map(PerformanceRecords::getOrderId)
                    .collect(Collectors.toList());

            // 获取订单Map (order_id -> Orders)
            Map<Long, Orders> ordersMap = orderService.getOrderDetails_1(orderIds)
                    .stream()
                    .collect(Collectors.toMap(Orders::getId, Function.identity()));

            // 4. 组装DTO
            List<AppealWithOrderDTO> result = appeals.stream()
                    .map(appeal -> {
                        PerformanceRecords performance = performanceMap.get(appeal.getPerformanceRecordId());
                        Orders order = performance != null ? ordersMap.get(performance.getOrderId()) : null;
                        return new AppealWithOrderDTO(appeal, order);
                    })
                    .collect(Collectors.toList());

            logger.info("成功获取申诉及关联订单，数量: {}", result.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取数据异常", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    @GetMapping("/staff/{staffId}")
    public ResponseEntity<List<AppealWithOrderDTO>> getAppealsByStaffId(
            HttpServletRequest request,
            @PathVariable Long staffId) {

        // 1. 权限验证
        if (!hasRole(request, "ADMIN")) {
            logger.warn("用户没有权限获取工作人员的申诉，请求被拒绝");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            // 2. 获取指定工作人员的申诉记录
            List<Appeals> appeals = appealService.getAppealsByStaffId(staffId);

            // 3. 批量获取关联数据（性能优化）
            List<Long> performanceIds = appeals.stream()
                    .map(Appeals::getPerformanceRecordId)
                    .collect(Collectors.toList());

            // 获取绩效记录Map (performance_id -> PerformanceRecords)
            Map<Long, PerformanceRecords> performanceMap = performanceService.getPerformanceRecordsDetails(performanceIds)
                    .stream()
                    .collect(Collectors.toMap(PerformanceRecords::getId, Function.identity()));

            // 获取订单ID列表
            List<Long> orderIds = performanceMap.values().stream()
                    .map(PerformanceRecords::getOrderId)
                    .collect(Collectors.toList());

            // 获取订单Map (order_id -> Orders)
            Map<Long, Orders> ordersMap = orderService.getOrderDetails_1(orderIds)
                    .stream()
                    .collect(Collectors.toMap(Orders::getId, Function.identity()));

            // 4. 组装DTO
            List<AppealWithOrderDTO> result = appeals.stream()
                    .map(appeal -> {
                        PerformanceRecords performance = performanceMap.get(appeal.getPerformanceRecordId());
                        Orders order = performance != null ? ordersMap.get(performance.getOrderId()) : null;
                        return new AppealWithOrderDTO(appeal, order);
                    })
                    .collect(Collectors.toList());

            logger.info("管理员成功获取工作人员 {} 的申诉及关联订单，数量: {}", staffId, result.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取工作人员的申诉及关联订单时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
