package com.example.order_project_1.services;

import com.example.order_project_1.models.entity.Appeals;
import com.example.order_project_1.models.entity.PerformanceRecords;
import gaarason.database.contract.eloquent.Record;
import gaarason.database.contract.eloquent.RecordList;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppealsService {

    @Resource
    private Appeals.Model appealsModel;

    @Resource
    private PerformanceRecords.Model performanceRecordsModel;

    // **提交申诉**
    public Appeals createAppeal(Long staffId, Long performanceRecordId, String reason) {
        // 获取绩效记录
        Record<PerformanceRecords, Long> recordOpt = performanceRecordsModel.newQuery().find(performanceRecordId);
        if (recordOpt == null || recordOpt.getEntity().getDegree() >= 4) {
            throw new RuntimeException("只有评分低于4的绩效记录才可以申诉");
        }

        // 插入申诉记录
        Appeals appeal = new Appeals();
        appeal.setStaffId(staffId);
        appeal.setPerformanceRecordId(performanceRecordId);
        appeal.setReason(reason);
        appeal.setStatus("待处理");
        appeal.setCreatedAt(LocalDateTime.now());

        Long insertedId = appealsModel.newQuery().insertGetId(appeal);
        if (insertedId != null) {
            Record<Appeals, Long> savedRecord = appealsModel.newQuery().find(insertedId);
            if (savedRecord != null) {
                return savedRecord.getEntity();
            }
        }
        return null;
    }

    // **管理员审批申诉**
    public Appeals reviewAppeal(Long appealId, boolean approve) {
        Record<Appeals, Long> appealRecord = appealsModel.newQuery().find(appealId);
        if (appealRecord == null) {
            throw new RuntimeException("申诉记录不存在");
        }

        Appeals appeal = appealRecord.getEntity();
        if (!"PENDING".equals(appeal.getStatus())) {
            throw new RuntimeException("该申诉已处理");
        }

        String newStatus = approve ? "APPROVED" : "REJECTED";
        appeal.setStatus(newStatus);
        appealRecord.save();

        if (approve) {
            // 更新绩效评分为 4
            Record<PerformanceRecords, Long> performanceRecord = performanceRecordsModel.newQuery().find(appeal.getPerformanceRecordId());
            if (performanceRecord != null) {
                PerformanceRecords record = performanceRecord.getEntity();
                record.setDegree(4.0);
                performanceRecord.save();
            }
        }
        return appeal;
    }

    // **获取所有申诉**
    public List<Appeals> getAllAppeals() {
        RecordList<Appeals, Long> records = appealsModel.newQuery().get();

        return records.stream().map(Record::getEntity).toList();
    }

    // **根据工作人员 ID 获取申诉**
    public List<Appeals> getAppealsByStaffId(Long staffId) {
        RecordList<Appeals, Long> records = appealsModel.newQuery().where("staff_id", staffId).get();
        return records.stream().map(Record::getEntity).toList();
    }
}
