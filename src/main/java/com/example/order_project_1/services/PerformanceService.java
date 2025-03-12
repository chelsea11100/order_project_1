package com.example.order_project_1.services;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import com.example.order_project_1.models.entity.Orders;
import com.example.order_project_1.models.entity.PerformanceRecords;
import gaarason.database.contract.eloquent.Record;
import gaarason.database.contract.eloquent.RecordList;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PerformanceService {

    @Resource
    PerformanceRecords.Model performanceRecordModel;
    private static final String OLLAMA_API_URL = "https://sc-lapp02.gcu.edu.cn/";
    private static final String MODEL_NAME = "deepseek-r1";

    @Resource
    private RestTemplate restTemplate;

    // 创建绩效记录（工作量AI来定义）
    public PerformanceRecords evaluateWorkload(Orders order) {
        String problemDescription = order.getProblemdescription();
        String feedback = order.getUserfeedback();
        Double workload = null;
        Double degree = null;
        Double salary;

        try {
            // 评估工作量
            JSONObject workloadRequestBody = new JSONObject();
            workloadRequestBody.put("model", MODEL_NAME);
            workloadRequestBody.put("prompt", "根据以下维护情况描述评估工作量（满分 5 分，返回 1 - 5 的数字）：" + problemDescription);
            workloadRequestBody.put("stream", false);

            HttpResponse workloadResponse = HttpRequest.post(OLLAMA_API_URL)
                    .header("Content-Type", "appliAcation/json; charset=UTF-8")
                    .body(workloadRequestBody.toString())
                    .execute();

            if (workloadResponse.isOk()) {
                String workloadResponseStr = workloadResponse.body();
                JSONObject workloadResponseJson = new JSONObject(workloadResponseStr);
                String workloadResultText = workloadResponseJson.getStr("response");
                Pattern workloadPattern = Pattern.compile("[1-5]");
                Matcher workloadMatcher = workloadPattern.matcher(workloadResultText);
                if (workloadMatcher.find()) {
                    workload = Double.parseDouble(workloadMatcher.group());
                }
            }

            if (workload == null) {
                return null;
            }

            // 评估满意度
            JSONObject degreeRequestBody = new JSONObject();
            degreeRequestBody.put("model", MODEL_NAME);
            degreeRequestBody.put("prompt", "根据以下用户对维修结果的评价评估满意度（满分 5 分，返回 1 - 5 的数字）：" + feedback);
            degreeRequestBody.put("stream", false);

            HttpResponse degreeResponse = HttpRequest.post(OLLAMA_API_URL)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(degreeRequestBody.toString())
                    .execute();

            if (degreeResponse.isOk()) {
                String degreeResponseStr = degreeResponse.body();
                JSONObject degreeResponseJson = new JSONObject(degreeResponseStr);
                String degreeResultText = degreeResponseJson.getStr("response");
                Pattern degreePattern = Pattern.compile("[1-5]");
                Matcher degreeMatcher = degreePattern.matcher(degreeResultText);
                if (degreeMatcher.find()) {
                    degree = Double.parseDouble(degreeMatcher.group());
                }
            }

            if (degree == null) {
                return null;
            }

            // 计算工资
            salary = 100 * workload + degree * 44;

            PerformanceRecords record = new PerformanceRecords();
            record.setStaffId(order.getStaffId());
            record.setCreatedat(LocalDateTime.now());
            record.setOrderId(order.getId());
            record.setWorkload(workload.toString());
            record.setDegree(Double.parseDouble(degree.toString()));
            record.setSalary(salary.toString());

            // 插入记录并获取插入记录的ID
            Long insertedId = performanceRecordModel.newQuery().insertGetId(record);
            if (insertedId != null) {
                // 根据ID查询插入的记录
                Record<PerformanceRecords, Long> savedRecord = performanceRecordModel.newQuery().find(insertedId);
                if (savedRecord != null) {
                    return savedRecord.getEntity();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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