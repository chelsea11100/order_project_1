package com.example.order_project_1.controllers;

import com.example.order_project_1.models.entity.Appeals;
import com.example.order_project_1.services.AppealsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/appeals")
public class AppealsController {

    @Resource
    private AppealsService appealsService;

    // **提交申诉**
    @PostMapping("/create")
    public ResponseEntity<Appeals> createAppeal(
            @RequestParam Long staffId,
            @RequestParam Long performanceRecordId,
            @RequestParam String reason) {
        Appeals appeal = appealsService.createAppeal(staffId, performanceRecordId, reason);
        return ResponseEntity.ok(appeal);
    }

    // **管理员审批申诉**
    @PostMapping("/review/{appealId}")
    public ResponseEntity<Appeals> reviewAppeal(
            @PathVariable Long appealId,
            @RequestParam boolean approve) {
        Appeals appeal = appealsService.reviewAppeal(appealId, approve);
        return ResponseEntity.ok(appeal);
    }

    // **获取所有申诉**
    @GetMapping("/all")
    public ResponseEntity<List<Appeals>> getAllAppeals() {
        List<Appeals> appeals = appealsService.getAllAppeals();
        return ResponseEntity.ok(appeals);
    }

    // **根据工作人员 ID 获取申诉**
    @GetMapping("/staff/{staffId}")
    public ResponseEntity<List<Appeals>> getAppealsByStaffId(@PathVariable Long staffId) {
        List<Appeals> appeals = appealsService.getAppealsByStaffId(staffId);
        return ResponseEntity.ok(appeals);
    }
}
