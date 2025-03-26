package com.example.order_project_1.services;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.example.order_project_1.models.entity.Orders;
import com.example.order_project_1.models.entity.PerformanceRecords;
import com.example.order_project_1.models.entity.Users;
import gaarason.database.contract.eloquent.Record;
import gaarason.database.contract.eloquent.RecordList;

import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PerformanceService {

    @Resource
    PerformanceRecords.Model performanceRecordModel;
    @Resource
    private Users.Model userModel;
    private static final String MODEL_NAME = "deepseek-r1:7b";
    private static final String OLLAMA_API_URL = "https://sc-lapp03.gcu.edu.cn/api/chat";
    // 部署好的 API 的 URL


    @Resource
    private RestTemplate restTemplate;

    // 创建绩效记录（工作量AI来定义）

    public PerformanceRecords evaluateWorkload(Orders order) {
        String problemDescription = order.getProblemdescription();
        String feedback = order.getUserfeedback();
        Double workload = null;
        Double degree = null;
        Double salary;//这个是总分

        try {
            System.out.println("已进入方法");
            // 评估工作量
            JSONObject workloadRequestBody = buildRequestBody("根据以下维护情况用数字（1~5分可以为小数）评估工作量：" + problemDescription);
            HttpResponse workloadResponse = HttpRequest.post(OLLAMA_API_URL)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(workloadRequestBody.toString())
                    .execute();
            System.out.println("workloadResponse.isOk(): "+workloadResponse.isOk());
            if (workloadResponse.isOk()) {
                String workloadResponseStr = workloadResponse.body();
                JSONObject workloadResponseJson = new JSONObject(workloadResponseStr);
                JSONObject message = workloadResponseJson.getJSONObject("message");
                String workloadResultText = message.getStr("content");
                System.out.println("workloadResultText: "+workloadResultText);
                String workloadResultText_1 = removeThinkTags(workloadResultText);
                System.out.println("workloadResultText_1: "+workloadResultText_1);


                if (workloadResultText_1 != null) {
                    Pattern workloadPattern = Pattern.compile("([1-5](\\.\\d+)?)");
                    Matcher workloadMatcher = workloadPattern.matcher(workloadResultText_1);
                    if (workloadMatcher.find()) {
                        workload = Double.parseDouble(workloadMatcher.group());
                    }
                }
            }
            System.out.println("workload:" + workload);

            if (workload == null) {
                return null;
            }

            // 评估满意度
            JSONObject degreeRequestBody = buildRequestBody("根据以下用户对维修结果的评价评估满意度返回一个数字（满分 5 分，返回 1 ~5 的数字可以为小数），只要一个数字：" + feedback);
            HttpResponse degreeResponse = HttpRequest.post(OLLAMA_API_URL)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(degreeRequestBody.toString())
                    .execute();
            if (degreeResponse.isOk()) {
                String degreeResponseStr = degreeResponse.body();
                JSONObject degreeResponseJson = new JSONObject(degreeResponseStr);
                JSONObject message = degreeResponseJson.getJSONObject("message");
                String degreeResultText = message.getStr("content");
                degreeResultText=removeThinkTags(degreeResultText);
                System.out.println(degreeResultText);
                if (degreeResultText != null) {
                    Pattern degreePattern = Pattern.compile("[1-5]");
                    Matcher degreeMatcher = degreePattern.matcher(degreeResultText);
                    if (degreeMatcher.find()) {
                        degree = Double.parseDouble(degreeMatcher.group());
                    }
                }
            }
            System.out.println("degree:" + degree);

            if (degree == null) {
                return null;
            }

            // 计算工资
            salary = workload + degree ;

            PerformanceRecords record = new PerformanceRecords();
            record.setStaffId(order.getStaffId());
            record.setCreatedat(LocalDateTime.now());
            record.setOrderId(order.getId());
            record.setWorkload(workload.toString());
            record.setDegree(degree);
            record.setSalary(salary);

            // 插入记录并获取插入记录的ID
            Long insertedId = performanceRecordModel.newQuery().insertGetId(record);
            if (insertedId != null) {
                // 根据ID查询插入的记录
                Record<PerformanceRecords, Long> savedRecord = performanceRecordModel.newQuery().find(insertedId);
                if (savedRecord != null) {
                    System.out.println("已获取到");
                    return savedRecord.getEntity();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private String removeThinkTags(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        int start = 0;

        while (true) {
            // 查找 <think> 标签的起始位置
            int thinkStart = input.indexOf("<think>", start);
            if (thinkStart == -1) {
                // 如果没有找到 <think>，将剩余部分添加到结果中并结束循环
                result.append(input.substring(start));
                break;
            }

            // 查找 </think> 标签的结束位置
            int thinkEnd = input.indexOf("</think>", thinkStart);
            if (thinkEnd == -1) {
                // 如果没有找到对应的 </think>，将剩余部分添加到结果中并结束循环
                result.append(input.substring(start));
                break;
            }

            // 将 <think> 标签之前的内容添加到结果中
            result.append(input.substring(start, thinkStart));

            // 跳过 <think>...</think> 的范围，继续查找下一个
            start = thinkEnd + "</think>".length();
        }

        return result.toString();
    }

    private JSONObject buildRequestBody(String prompt) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", MODEL_NAME);

        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);
        requestBody.put("messages", messages);

        requestBody.put("stream", false);

        JSONObject format = new JSONObject();
        JSONObject properties = new JSONObject();
        JSONObject scoreProp = new JSONObject();
        scoreProp.put("type", "integer");
        properties.put("score", scoreProp);
        format.put("properties", properties);
        format.put("required", new String[]{"score"});
        format.put("type", "object");
        requestBody.put("format", format);

        JSONObject options = new JSONObject();
        options.put("temperature", 0);
        requestBody.put("options", options);

        return requestBody;
    }


    // 获取工作人员的绩效记录
    public List<PerformanceRecords> getPerformanceRecordsByStaffId(Long staffId) {
        RecordList<PerformanceRecords, Long> records = performanceRecordModel.newQuery()
                .where("staff_id", staffId)
                .get();
        return records.stream().map(Record::getEntity).toList();
    }

    //admin获取所有工作人员的绩效
    public List<PerformanceRecords> getAllPerformanceRecords() {
        // 1. 创建查询（不添加任何过滤条件）
        RecordList<PerformanceRecords, Long> records = performanceRecordModel.newQuery()
                .get(); // 直接获取所有记录

        // 2. 转换为实体列表（与 getPerformanceRecordsByStaffId 保持格式一致）
        return records.stream()
                .map(Record::getEntity)
                .toList();
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
    @Scheduled(cron = "0 0 1 1 * ?")  // 每月1号凌晨1点执行
    public void updateMonthlyPerformance() {
        // 1. 获取所有员工（角色为STAFF）
        List<Users> staffUsers = userModel.newQuery()
                .where("role", "STAFF")  // 筛选角色为STAFF的员工
                .get()
                .stream()
                .map(Record::getEntity)  // 转换为实体类
                .toList();

        // 2. 遍历每个员工，计算平均绩效
        for (Users user : staffUsers) {
            Long staffId = user.getId();

            // 3. 查询该员工的所有薪资记录
            List<PerformanceRecords> performanceRecords = performanceRecordModel.newQuery()
                    .where("staff_id", staffId)
                    .get()
                    .stream()
                    .map(Record::getEntity)  // 转换为实体类
                    .toList();

            // 4. 计算平均薪资
            double totalSalary = 0.0;
            int count = 0;
            for (PerformanceRecords record : performanceRecords) {
                totalSalary +=record.getSalary();
                count++;
            }
            Double avgSalary = (count > 0) ? totalSalary / count : null;

            // 5. 更新用户表的总绩效
            if (avgSalary != null) {
                // 重新查询用户记录（确保操作最新数据）
                Record<Users, Long> currentUserRecord = userModel.newQuery().find(staffId);
                if (currentUserRecord != null) {
                    Users updatedUser = currentUserRecord.getEntity();
                    updatedUser.setTotal_performance(avgSalary);
                    currentUserRecord.save();  // 保存更新
                    System.out.println("✅ 员工ID=" + staffId + "，平均绩效已更新为：" + avgSalary);
                } else {
                    System.err.println("⚠️ 用户记录不存在，ID=" + staffId);
                }
            } else {
                System.out.println("员工ID=" + staffId + " 无有效绩效记录");
            }
        }

        System.out.println("月度绩效更新完成");
    }
}