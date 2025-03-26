package com.example.order_project_1.services;
import com.example.order_project_1.models.entity.PerformanceRecords;
import com.example.order_project_1.models.entity.Users;
import com.example.order_project_1.models.entity.Orders;
import gaarason.database.appointment.OrderBy;
import gaarason.database.contract.eloquent.Record;
import gaarason.database.contract.eloquent.RecordList;
import gaarason.database.eloquent.RecordBean;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
@Service
public class OrderService {

    @Resource
    private Orders.Model orderModel;

    @Resource
    private PerformanceRecords.Model performanceModel;
    @Resource
    private Users.Model userModel;

    // 用户创建订单
    public Orders createOrder(Orders order) {
        // 获取当前时间
        LocalDateTime currentTime = LocalDateTime.now();
        // 设置创建时间
        order.setCreate(currentTime);
        // 使用 insertGetId 方法插入记录并获取插入记录的主键 ID
        Long insertedId = orderModel.newQuery().insertGetId(order);
        if (insertedId != null) {
            // 根据主键 ID 查询插入的记录
            Record<Orders, Long> savedRecord = orderModel.newQuery().find(insertedId);
            if (savedRecord != null) {
                return savedRecord.getEntity();
            }
        }
        return null;
    }

    // 根据用户id查询订单
    public List<Orders> getOrdersByUserId(Long userId) {
        RecordList<Orders, Long> records = orderModel.newQuery().where("user_id", userId).get();
        return records.stream().map(Record::getEntity).toList();
    }

    // 根据工作人员id查询订单
    public List<Orders> getOrdersByStaffId(Long staffId) {
        RecordList<Orders, Long> records = orderModel.newQuery().where("staff_id", staffId).get();
        return records.stream().map(Record::getEntity).toList();
    }

    //管理员查询所有订单
    public List<Orders> getAllOrders() {
        RecordList<Orders, Long> records = orderModel.newQuery().get();
        return records.stream().map(Record::getEntity).collect(Collectors.toList());
    }

    // 更新订单状态
    public Orders updateOrderStatus(Long orderId, String status) {
        Record<Orders, Long> orderRecord = orderModel.newQuery().find(orderId);
        if (orderRecord != null) {
            Orders order = orderRecord.getEntity();
            order.setStatus(status);
            orderRecord.save();
            return order;
        }
        return null;
    }

    // 根据订单状态查找订单
    public List<Orders> getOrdersByStatus(String status) {
        RecordList<Orders, Long> records = orderModel.newQuery().where("status", status).get();
        return records.stream().map(Record::getEntity).toList();
    }

    // 用户查询历史订单

    public List<Orders> getOrderHistory(Long userId) {
        RecordList<Orders, Long> records = orderModel.newQuery()
                .where("user_id", userId)
                .where("status", "<>", "已取消")
                .get();
        return records.stream().map(Record::getEntity).toList();
    }

    //工作人员查询历史订单
    public List<Orders> getOrderHistory_1(Long userId) {
        RecordList<Orders, Long> records = orderModel.newQuery()
                .where("staff_id", userId)
                .where("status", "<>", "已取消")
                .get();
        return records.stream().map(Record::getEntity).toList();
    }


    // 查看订单详情
    public Orders getOrderDetails(Long orderId) {
        Record<Orders, Long> orderRecord = orderModel.newQuery().find(orderId);
        return orderRecord != null ? orderRecord.getEntity() : null;
    }

    // 取消订单
    public boolean cancelOrder(Long orderId, Long userId, String role) {
        Record<Orders, Long> orderRecord = orderModel.newQuery().find(orderId);
        if (orderRecord != null) {
            Orders existingOrder = orderRecord.getEntity();
            if ("USER".equals(role)) {
                if ("待处理".equals(existingOrder.getStatus())) {
                    existingOrder.setStatus("已取消");
                    orderRecord.save();
                    return true;
                }
            } else if ("ADMIN".equals(role)) {
                boolean isDuplicateOrder = orderModel.newQuery()
                        .where("id", orderId)
                        .where("user_id", "<>", userId)
                        .first() != null;
                if (isDuplicateOrder) {
                    existingOrder.setStatus("已取消");
                    orderRecord.save();
                    return true;
                }
            }
        }
        return false;
    }

    // 用户评价订单
    public void submitOrderFeedback(Long orderId, String feedback, String result, Long userId) {
        // 查询订单
        Record<Orders, Long> orderRecord = orderModel.newQuery().find(orderId);
        if (orderRecord == null) {
            throw new IllegalArgumentException("订单不存在");
        }

        // 验证订单的所有者是否为当前用户
        Orders existingOrder = orderRecord.getEntity();
        if (!existingOrder.getUserId().equals(userId)) {
            throw new IllegalArgumentException("用户无权操作此订单");
        }

        // 更新订单的反馈和结果
        existingOrder.setResult(result);
        existingOrder.setUserfeedback(feedback);
        orderRecord.save();
    }

    // **自动派单**
    public void autoAssignOrder(Orders order, Set<Long> assignedStaffIds) {
        if (order.getStaffId() != null) {
            return; // 订单已被接单，无需派单
        }

        LocalDateTime now = LocalDateTime.now();
        if (Duration.between(order.getCreate(), now).toHours() < 9) {
            return; // 订单未超时，不触发 AI 派单
        }

        // **获取所有技术人员**
        List<Users> staffList = userModel.newQuery()
                .where("role", "STAFF")
                .get()
                .stream()
                .map(Record::getEntity)
                .filter(staff -> !assignedStaffIds.contains(staff.getId())) // **过滤掉已分配的技术人员**
                .toList();

        if (staffList.isEmpty()) {
            System.out.println("没有可用的技术人员");
            return; // 所有技术人员都已经接单，退出
        }

        // **计算 AI 评分，选出最优工作人员**
        Optional<Users> bestStaff = staffList.stream()
                .map(staff -> new StaffScore(staff, calculateAIScore(staff)))
                .max(Comparator.comparingDouble(StaffScore::score))
                .map(StaffScore::staff);

        // **分配工单**
        bestStaff.ifPresent(assignedStaff -> {
            Orders updatedOrder = new Orders();
            updatedOrder.setStaffId(assignedStaff.getId());
            updatedOrder.setAccepted(LocalDateTime.now());
            updatedOrder.setStatus("已接单");

            orderModel.newQuery().where("id", order.getId()).update(updatedOrder);
            // **标记该技术员已经被分配**
            assignedStaffIds.add(assignedStaff.getId());
        });
    }

    // **AI 评分计算逻辑**
    private double calculateAIScore(Users staff) {
        long orderCount = getOrderCount(staff.getId());
        double avgDifficulty = getAvgDifficulty(staff.getId());
        long idleTime = getIdleTime(staff.getId());
        double avgQuality = getAvgQuality(staff.getId());

        return (orderCount * 0.3) + (avgDifficulty * 0.2) + (idleTime * 0.3) + (avgQuality * 0.2);
    }

    // **获取工作人员接单数量（去重）**
    private long getOrderCount(Long staffId) {
        // 查询并获取结果，给 COUNT(DISTINCT order_id) 设置别名
        Object result = performanceModel.newQuery()
                .where("staff_id", staffId)
                .selectRaw("COUNT(DISTINCT order_id) AS order_count")
                .first();

        if (result != null && result instanceof RecordBean) {
            // 将结果转换为 RecordBean 类型
            RecordBean record = (RecordBean) result;

            // 假设 RecordBean 提供了 toMap() 方法
            Map<String, Object> resultMap = record.toMap();

            // 从 Map 中获取 'order_count' 字段的值
            Object countValue = resultMap.get("order_count");

            // 将值转换为 Long 类型并返回
            return countValue != null ? Long.parseLong(countValue.toString()) : 0L;
        }

        return 0L;
    }

    // **计算工作人员平均订单难度（去重）**
    private double getAvgDifficulty(Long staffId) {
        // 查询并获取结果，给 AVG(workload) 设置别名
        Object result = performanceModel.newQuery()
                .where("staff_id", staffId)
                .selectRaw("AVG(workload) AS avg_workload")
                .first();

        if (result != null && result instanceof RecordBean) {
            // 将 RecordBean 转换为 Map
            RecordBean record = (RecordBean) result;
            Map<String, Object> resultMap = record.toMap();

            // 获取 "avg_workload" 字段的值
            Object countValue = resultMap.get("avg_workload");

            if (countValue != null) {
                // 将值转换为 Double 类型并返回
                return Double.parseDouble(countValue.toString());
            }
        }
        return 0.0; // 如果没有找到字段或转换失败，返回 0.0
    }

    // **计算工作人员空窗时间**
    private long getIdleTime(Long staffId) {
        Record<Orders, Long> lastOrder = orderModel.newQuery()
                .where("staff_id", staffId)
                .whereNotNull("completedAt")
                .orderBy("completedAt", OrderBy.DESC)
                .first();

        if (lastOrder == null || lastOrder.getEntity().getCompleted() == null) {
            return Long.MAX_VALUE;
        }

        return Duration.between(lastOrder.getEntity().getCompleted(), LocalDateTime.now()).toHours();
    }

    // **计算工作人员的平均服务质量评分（去重）**
    private double getAvgQuality(Long staffId) {
        Object avgQualityObj = performanceModel.newQuery()
                .where("staff_id", staffId)
                .group("order_id")
                .selectRaw("AVG(degree) AS avg_degree")  // 给字段起个别名，避免字段名冲突
                .first();

        if (avgQualityObj != null) {
            // 使用 RecordBean 时，先转换为 Map 获取值
            if (avgQualityObj instanceof RecordBean) {
                RecordBean record = (RecordBean) avgQualityObj;
                Map<String, Object> resultMap = record.toMap();

                Object avgDegree = resultMap.get("avg_degree");  // 获取查询结果中的值

                if (avgDegree instanceof Number) {
                    // 将值转换为 double 并返回
                    return ((Number) avgDegree).doubleValue();
                }
            }
        }
        // 如果结果为空，则返回 0.0
        return 0.0;
    }

    // **内部类：存储工作人员及其评分**
    private record StaffScore(Users staff, double score) {
    }

    // **定时任务：每 30 分钟检查一次超时订单并执行 AI 自动派单**
    @Scheduled(fixedRate = 30 * 60 * 1000) // 每 30 分钟执行一次
    public void checkAndAssignOrders() {
        System.out.println("正在检查超时订单...");

        // 查询创建时间超过 9 小时且无人接单的订单
        List<Orders> pendingOrders = orderModel.newQuery()
                .whereNull("staff_id") // 还未分配工作人员
                .whereRaw("TIMESTAMPDIFF(HOUR, createdAt, NOW()) >= 9") // 计算订单创建时间
                .get()
                .stream()
                .map(Record::getEntity)
                .toList();

        // **用于存储已分配的技术人员，避免重复指派**
        Set<Long> assignedStaffIds = new HashSet<>();
        for (Orders order : pendingOrders) {
            System.out.println("正在为订单 " + order.getId() + " 进行自动派单...");
            autoAssignOrder(order, assignedStaffIds); // 调用 AI 派单逻辑
        }
        System.out.println("自动派单任务完成");
    }

    @Scheduled(fixedRate = 30 * 1000) // 30秒
    public void autoUpdateUserStatus() {
        // 查询所有状态为“空闲”或“接单中”的用户
        List<Users> users = userModel.newQuery()
                .whereIn("user_status", List.of("空闲", "接单中"))
                .get()
                .stream()
                .map(Record::getEntity)
                .toList();

        // 遍历处理每个用户
        for (Users user : users) {
            updateStatusBasedOnOrders(user);
        }
    }

    private void updateStatusBasedOnOrders(Users user) {
        Long staffId = user.getId();
        String currentStatus = user.getUser_status();

        // 查询该用户的所有订单
        List<Orders> orders = orderModel.newQuery()
                .where("staff_id", staffId)
                .get()
                .stream()
                .map(Record::getEntity)
                .toList();

        // 根据订单状态更新用户状态
        if ("空闲".equals(currentStatus)) {
            handleIdleUser(user, orders);
        } else if ("接单中".equals(currentStatus)) {
            handleBusyUser(user, orders);
        }
    }

    /**
     * 处理状态为“空闲”的用户
     */
    private void handleIdleUser(Users user, List<Orders> orders) {
        // 检查是否存在至少一个“已接单”订单
        boolean hasAcceptedOrder = orders.stream()
                .anyMatch(order -> "已接单".equals(order.getStatus()));

        if (hasAcceptedOrder) {
            // 通过用户ID查询用户记录
            Record<Users, Long> userRecord = userModel.newQuery().find(user.getId());
            if (userRecord != null) {
                // 获取用户实体并更新状态
                Users updatedUser = userRecord.getEntity();
                updatedUser.setUser_status("接单中");

                // 保存更新后的记录
                userRecord.save();
                System.out.println("✅ 用户 " + user.getId() + " 状态更新为 [接单中]");
            } else {
                System.err.println("⚠️ 用户记录不存在，ID=" + user.getId());
            }
        }
    }

    /**
     * 处理状态为“接单中”的用户
     */
    private void handleBusyUser(Users user, List<Orders> orders) {
        // 检查是否所有订单都不是“已接单”状态
        boolean allOrdersNotAccepted = orders.stream()
                .allMatch(order -> !"已接单".equals(order.getStatus()));

        if (allOrdersNotAccepted) {
            // 通过用户ID查询用户记录
            Record<Users, Long> userRecord = userModel.newQuery().find(user.getId());
            if (userRecord != null) {
                // 获取用户实体并更新状态
                Users updatedUser = userRecord.getEntity();
                updatedUser.setUser_status("空闲");

                // 保存更新后的记录
                userRecord.save();
                System.out.println("✅ 用户 " + user.getId() + " 状态更新为 [空闲]");
            } else {
                System.err.println("⚠️ 用户记录不存在，ID=" + user.getId());
            }
        }
    }
}