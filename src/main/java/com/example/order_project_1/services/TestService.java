package com.example.order_project_1.services;

import com.example.order_project_1.models.entity.Orders;
import com.example.order_project_1.models.entity.Users;
import gaarason.database.contract.eloquent.Record;
import gaarason.database.contract.eloquent.RecordList;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestService {

    @Resource
    Users.Model users;

    public void test()
    {
        RecordList<Users,Long> record=users.newQuery().get();
        List<Record<Users,Long>> list=record.stream().toList();
        for (Record<Users, Long> usersLongRecord : list) {
            System.out.println(usersLongRecord.getEntity().getCreatedat());
        }
    }
}
