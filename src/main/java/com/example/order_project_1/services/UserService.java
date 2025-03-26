package com.example.order_project_1.services;


import com.example.order_project_1.models.entity.Orders;
import com.example.order_project_1.models.entity.Users;
import gaarason.database.contract.eloquent.Record;
import gaarason.database.contract.eloquent.RecordList;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Resource
    private Users.Model userModel;

    @Resource
    private Orders.Model orderModel;

    // 用户注册
    public Users registerUser(Users user) {
        if (user.getContactinfo() == null) {
            user.setContactinfo("未提供联系电话");
        }
        user.setCreatedat(LocalDateTime.now());

        // 使用 insertGetId 方法获取插入记录的 ID
        Long insertedId = userModel.newQuery().insertGetId(user);

        if (insertedId != null) {
            // 根据 ID 查询插入的记录
            Record<Users, Long> savedRecord = userModel.newQuery().find(insertedId);
            if (savedRecord != null) {
                return savedRecord.getEntity();
            }
        }
        return null;
    }

    // 用户登录
    public Optional<Users> loginUser(String username, String password) {
        // 查询符合用户名的记录列表
        RecordList<Users, Long> userRecords = userModel.newQuery().where("username", username).get();
        if (!userRecords.isEmpty()) {
            // 取第一条记录
            Record<Users, Long> userRecord = userRecords.get(0);
            if (password.equals(userRecord.getEntity().getPassword())) {
                return Optional.of(userRecord.getEntity());
            }
        }
        return Optional.empty();
    }

    // 管理员管理工作人员
    public Users addStaff(Users user) {
        user.setRole("STAFF");
        user.setCreatedat(LocalDateTime.now());
        // 使用 insertGetId 方法获取插入记录的 ID
        Long insertedId = userModel.newQuery().insertGetId(user);
        if (insertedId != null) {
            // 根据 ID 查询插入的记录
            Record<Users, Long> savedRecord = userModel.newQuery().find(insertedId);
            if (savedRecord != null) {
                return savedRecord.getEntity();
            }
        }
        return null;
    }

    public void deleteStaff(Long staffId) {
        userModel.newQuery().where("id", staffId).delete();
    }


    public Users getStaff(Long staffId) {
        Record<Users, Long> userRecord = userModel.newQuery().find(staffId);
        return userRecord != null ? userRecord.getEntity() : null;
    }

    //管理人员查看所有工作人员信息
    public List<Users> getStaffs(){
        RecordList<Users,Long> staffRecords = userModel.newQuery().where("role","STAFF").get();
        return staffRecords.stream().map(Record::getEntity).toList();
    }

    // 管理员和工作人员查看所有未接订单及管理员手动派单
    public List<Orders> findUnassignedOrders() {
        RecordList<Orders, Long> orderRecords = orderModel.newQuery().where("status", "待处理").get();
        return orderRecords.stream().map(Record::getEntity).toList();
    }

    public Orders assignOrderToStaff(Long orderId, Long staffId) {
        Record<Orders, Long> orderRecord = orderModel.newQuery().find(orderId);
        if (orderRecord != null) {
            Orders order = orderRecord.getEntity();
            order.setStaffId(staffId);
            orderRecord.save();
            return order;
        }
        return null;
    }

    // 管理员查看和修改自己的信息
    public Users getAdminProfile(Long adminId) {
        Record<Users, Long> userRecord = userModel.newQuery().find(adminId);
        return userRecord != null ? userRecord.getEntity() : null;
    }

    public Users updateAdminProfile(Long adminId, Users userDetails) {
        Record<Users, Long> existingRecord = userModel.newQuery().find(adminId);
        if (existingRecord != null) {
            Users currentUser = existingRecord.getEntity();
            currentUser.setUsername(userDetails.getUsername());
            currentUser.setSpecialty(userDetails.getSpecialty());
            currentUser.setContactinfo(userDetails.getContactinfo());
            currentUser.setAvatar(userDetails.getAvatar());
            existingRecord.save();
            return currentUser;
        }
        return null;
    }

    // 工作人员查看和修改自己的信息
    public Users getStaffProfile(Long staffId) {
        Record<Users, Long> userRecord = userModel.newQuery().find(staffId);
        return userRecord != null ? userRecord.getEntity() : null;
    }



    // 更新用户信息
    public Users updateUserProfile(Long userId, Users newUserDetails) {
        Record<Users, Long> existingRecord = userModel.newQuery().find(userId);
        if (existingRecord != null) {
            Users currentUser = existingRecord.getEntity();
            if (newUserDetails.getUsername() != null) {
                currentUser.setUsername(newUserDetails.getUsername());
            }
            if (newUserDetails.getContactinfo() != null) {
                currentUser.setContactinfo(newUserDetails.getContactinfo());
            }
            if (newUserDetails.getSpecialty() != null) {
                currentUser.setSpecialty(newUserDetails.getSpecialty());
            }
            if (newUserDetails.getAvatar() != null) {
                currentUser.setAvatar(newUserDetails.getAvatar());
            }
            existingRecord.save();
            return currentUser;
        }
        return null;
    }

    public Users getUserProfile(Long userId) {
        Record<Users, Long> userRecord = userModel.newQuery().find(userId);
        return userRecord != null ? userRecord.getEntity() : null;
    }

    // 新增方法：根据用户名查找用户
    public Optional<Users> findUserByUsername(String username) {
        RecordList<Users, Long> userRecords = userModel.newQuery().where("username", username).get();
        if (!userRecords.isEmpty()) {
            return Optional.of(userRecords.get(0).getEntity());
        }
        return Optional.empty();
    }
}