package com.example.order_project_1.services;
import com.example.order_project_1.models.entity.Users;
import com.example.order_project_1.models.entity.Orders;
import gaarason.database.appointment.OrderBy;
import gaarason.database.contract.eloquent.Record;
import gaarason.database.contract.eloquent.RecordList;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.time.Duration;
import java.util.Comparator;
import java.math.BigDecimal;
import org.springframework.scheduling.annotation.Scheduled;
@Service
public class OrderService {

    @Resource
    private Orders.Model orderModel;
    @Resource
    private Users.Model userModel;
    // 用户创建订单
    public Orders createOrder(Orders order) {
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

    // 查询历史订单

    public List<Orders> getOrderHistory(Long userId) {
        RecordList<Orders, Long> records = orderModel.newQuery()
                .where("user_id", userId)
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
    public void submitOrderFeedback(Long orderId, String feedback, String result) {
        Record<Orders, Long> orderRecord = orderModel.newQuery().find(orderId);
        if (orderRecord != null) {
            Orders existingOrder = orderRecord.getEntity();
            existingOrder.setResult(result);
            existingOrder.setUserfeedback(feedback);
            orderRecord.save();
        }
    }

    // AI自动派单
    public void autoAssignOrder(Orders order) {
        if (order.getAccepted() != null) {
            return; // 订单已被接单，无需派单
        }

        LocalDateTime now = LocalDateTime.now();
        if (Duration.between(order.getCreate(), now).toHours() < 9) {
            return; // 订单未超时，不触发 AI 派单
        }

        // 获取所有技术人员（role = "STAFF"）
        List<Users> staffList = userModel.newQuery().where("role", "STAFF").get().stream()
                .map(Record::getEntity).toList();
        // 计算 AI 评分并选出最佳工作人员
        Optional<Users> bestStaff = staffList.stream()
                .map(staff -> new StaffScore(staff, calculateAIScore(staff)))
                .max(Comparator.comparingDouble(StaffScore::score))
                .map(StaffScore::staff);

        if (bestStaff.isPresent()) {
            Users assignedStaff = bestStaff.get();
            order.setStaffId(assignedStaff.getId());
            order.setAccepted(LocalDateTime.now());
            order.setStatus("ASSIGNED");
            // 更新数据库
            orderModel.newQuery().where("id", order.getId()).update(order);
        }
    }
    // **AI 计算评分逻辑**
    private double calculateAIScore(Users staff) {
        Long orderCount = getOrderCount(staff.getId());
        double avgDifficulty = getAvgDifficulty(staff.getId());
        long idleTime = getIdleTime(staff.getId());
        double avgQuality = getAvgQuality(staff.getId());

        return (orderCount * 0.3) + (avgDifficulty * 0.2) + (idleTime * 0.3) + (avgQuality * 0.2);
    }
    // **获取工作人员接单数量**
    private long getOrderCount(Long staffId) {
        return  orderModel.newQuery().where("staff_id", staffId).count();
    }
    // **计算工作人员平均订单难度**
    private double getAvgDifficulty(Long staffId) {
        // avg() 返回 BigDecimal，需要转换为 double
        BigDecimal avgDifficulty = orderModel.newQuery().where("staff_id", staffId).avg("workload");
        return avgDifficulty != null ? avgDifficulty.doubleValue() : 0.0;
    }
    // **计算工作人员空窗时间**
    private long getIdleTime(Long staffId) {
        Record<Orders, Long> lastOrder = orderModel.newQuery()
                .where("staff_id", staffId)
                .orderBy("completedAt", OrderBy.valueOf("desc"))
                .first();

        if (lastOrder == null || lastOrder.getEntity().getCompleted() == null) {
            return Long.MAX_VALUE;
        }

        return Duration.between(lastOrder.getEntity().getCompleted(), LocalDateTime.now()).toHours();
    }
    // **计算工作人员的平均服务质量评分**
    private double getAvgQuality(Long staffId) {
        // avg() 返回 BigDecimal，需要转换为 double
        BigDecimal avgQuality = orderModel.newQuery().where("staff_id", staffId).avg("degree");
        return avgQuality != null ? avgQuality.doubleValue() : 0.0;
    }
    // **内部类：存储工作人员及其评分**
    private record StaffScore(Users staff, double score) {}
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

        for (Orders order : pendingOrders) {
            System.out.println("正在为订单 " + order.getId() + " 进行 AI 自动派单...");
            autoAssignOrder(order); // 调用 AI 派单逻辑
        }

        System.out.println("AI 自动派单任务完成");
    }
}