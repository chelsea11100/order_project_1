package com.example.order_project_1.services;

import com.example.order_project_1.models.entity.Orders;
import gaarason.database.contract.eloquent.Model;
import gaarason.database.contract.eloquent.Record;
import gaarason.database.contract.eloquent.RecordList;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Resource
    private Orders.Model orderModel;

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

    public void handleOrderAppeal(Long orderId, String appealReason) {
        // 记录订单申诉逻辑
    }

    public void autoAssignOrder(Orders order) {
        // AI自动派单逻辑
    }
}