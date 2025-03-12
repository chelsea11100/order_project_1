package com.example.order_project_1.controllers;

import com.example.order_project_1.models.entity.PerformanceRecords;
import com.example.order_project_1.services.PerformanceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/performance")
public class PerformanceController {
    @Autowired
    private PerformanceService performanceService;

    // 模拟当前用户角色，实际应用中需要从会话或请求中获取
    private boolean getCurrentUserRole(HttpServletRequest request, String role) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            // 假设登录时将用户角色信息存入会话，键为 "role"
            String userRole = (String) session.getAttribute("role");
            if (userRole != null) {
                return userRole.equals(role);
            }
        }
        return false;
    }

    //获取工作人员绩效记录
    @GetMapping("/staff/{staffId}")
    public ResponseEntity<List<PerformanceRecords>> getPerformanceRecordsByStaffId(@PathVariable Long staffId,HttpServletRequest request) {

        if (getCurrentUserRole(request, "ADMIN")&& getCurrentUserRole(request, "STAFF")) {
            return ResponseEntity.status(403).build(); // 403 表示禁止访问
        }
        List<PerformanceRecords> records = performanceService.getPerformanceRecordsByStaffId(staffId);
        return ResponseEntity.ok(records);
    }

    //修改绩效记录
    @PutMapping("/{performanceId}")
    public ResponseEntity<PerformanceRecords> updatePerformance(@PathVariable Long performanceId, @RequestBody PerformanceRecords record,HttpServletRequest request) {

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

    //处理绩效申诉
    @PostMapping("/{performanceId}/appeal")
    public ResponseEntity<Void> handlePerformanceAppeal(@PathVariable Long performanceId, @RequestBody String appealReason) {
        // 这里可以根据需要添加角色判断，如果不需要角色限制可以不添加
        performanceService.handlePerformanceAppeal(performanceId, appealReason);
        return ResponseEntity.noContent().build();
    }
}
