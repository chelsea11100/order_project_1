package com.example.order_project_1.services;

import com.example.order_project_1.models.entity.Orders;
import com.example.order_project_1.models.entity.PerformanceRecords;
import gaarason.database.contract.eloquent.Record;
import gaarason.database.contract.eloquent.RecordList;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
public class PerformanceService {

    @Resource
    PerformanceRecords.Model performanceRecordModel;

    @Resource
    private RestTemplate restTemplate;

    // 创建绩效记录（工作量AI来定义）
    public PerformanceRecords evaluateWorkload(Orders order) {
        String aiUrl = "http://xxxxx.com/evaluate";
        String feedback = order.getUserfeedback();
        Double workload;

        ResponseEntity<Double> response = restTemplate.getForEntity(
                UriComponentsBuilder.fromHttpUrl(aiUrl)
                        .queryParam("feedback", feedback)
                        .build().toUri(),
                Double.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            workload = response.getBody();
        } else {
            return null;
        }

        PerformanceRecords record = new PerformanceRecords();
        record.setOrderId(order.getId()); // 假设Order类有getId方法
        record.setWorkload(workload.toString());
        record.setSalary(String.valueOf(workload * 100));

        // 插入记录并获取插入记录的ID
        Long insertedId = performanceRecordModel.newQuery().insertGetId(record);
        if (insertedId != null) {
            // 根据ID查询插入的记录
            Record<PerformanceRecords, Long> savedRecord = performanceRecordModel.newQuery().find(insertedId);
            if (savedRecord != null) {
                return savedRecord.getEntity();
            }
        }
        return null;
    }

    // 获取工作人员的绩效记录
    public List<PerformanceRecords> getPerformanceRecordsByStaffId(Long staffId) {
        RecordList<PerformanceRecords, Long> records = performanceRecordModel.newQuery()
                .where("staff_id", staffId)
                .get();
        return records.stream().map(Record::getEntity).toList();
    }

    public PerformanceRecords updatePerformance(Long performanceId, Double newWorkload) {
        Record<PerformanceRecords, Long> record = performanceRecordModel.newQuery().find(performanceId);
        if (record != null) {
            PerformanceRecords entity = record.getEntity();
            entity.setWorkload(newWorkload.toString());
            record.save();
            return entity;
        }
        return null;
    }

    public void handlePerformanceAppeal(Long performanceId, String appealReason) {
        // 处理绩效申诉逻辑
    }
}