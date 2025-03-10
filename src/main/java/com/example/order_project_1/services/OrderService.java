//package com.example.order_project_1.services;
//
//import com.example.order_project_1.models.entity.Orders;
//import gaarason.database.contract.eloquent.Record;
//import gaarason.database.contract.eloquent.RecordList;
//import jakarta.annotation.Resource;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Optional;
//
//@Service
//public class OrderService {
//
//    @Resource
//    private Orders.Model orders;
//
//    // 用户创建订单
//    public Orders createOrder(Orders order) {
//        return orders.newQuery().save(order);
//    }
//
//    // 根据用户ID查询订单
//    public List<Orders> getOrdersByUserId(Long userId) {
//        RecordList<Orders, Long> records = orders.newQuery().where("user_id", userId).get();
//        return records.stream().map(Record::getEntity).toList();
//    }
//
//    // 根据工作人员ID查询订单
//    public List<Orders> getOrdersByStaffId(Long staffId) {
//        RecordList<Orders, Long> records = orders.newQuery().where("staff_id", staffId).get();
//        return records.stream().map(Record::getEntity).toList();
//    }
//
//    // 更新订单状态
//    public Orders updateOrderStatus(Long orderId, String status) {
//        Optional<Record<Orders, Long>> record = Optional.of(orders.newQuery().findOrFail(orderId));
//        if (record.isPresent()) {
//            Orders order = record.get().getEntity();
//            order.setStatus(status);
//            return orders.newQuery().save(order);
//        }
//        return null;
//    }
//
//    // 根据订单状态查找订单
//    public List<Orders> getOrdersByStatus(String status) {
//        RecordList<Orders, Long> records = orders.newQuery().where("status", status).get();
//        return records.stream().map(Record::getEntity).toList();
//    }
//
//    // 查询历史订单
//    public List<Orders> getOrderHistory(Long userId) {
//        RecordList<Orders, Long> records = orders.newQuery()
//                .where("user_id", userId)
//                .whereNot("status", "已取消")
//                .get();
//        return records.stream().map(Record::getEntity).toList();
//    }
//
//    // 查看订单详情
//    public Orders getOrderDetails(Long orderId) {
//        Optional<Record<Orders, Long>> record = orders.newQuery().findOrFail(orderId);
//        return record.map(Record::getEntity).orElse(null);
//    }
//
//    // 取消订单
//    public boolean cancelOrder(Long orderId, Long userId, String role) {
//        Optional<Record<Orders, Long>> record = orders.newQuery().findOrFail(orderId);
//        if (record.isPresent()) {
//            Orders order = record.get().getEntity();
//            if ("USER".equals(role) {
//                if ("待处理".equals(order.getStatus())) {
//                    order.setStatus("已取消");
//                    orders.newQuery().save(order);
//                    return true;
//                }
//            } else if ("ADMIN".equals(role)) {
//                boolean isDuplicateOrder = orders.newQuery()
//                        .where("id", orderId)
//                        .whereNot("user_id", userId)
//                        .exists();
//                if (isDuplicateOrder) {
//                    order.setStatus("已取消");
//                    orders.newQuery().save(order);
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
//
//    // 用户评价订单
//    public void submitOrderFeedback(Long orderId, String feedback, String result) {
//        Optional<Record<Orders, Long>> record = Optional.of(orders.newQuery().findOrFail(orderId));
//        if (record.isPresent()) {
//            Orders order = record.get().getEntity();
//            order.setResult(result);
//            order.setUserFeedback(feedback);
//            orders.newQuery().save(order);
//        }
//    }
//
//    // 处理订单申诉
//    public void handleOrderAppeal(Long orderId, String appealReason) {
//        // 记录订单申诉逻辑
//    }
//
//    // 自动派单
//    public void autoAssignOrder(Orders order) {
//        // AI自动派单逻辑
//    }
//}
