package com.example.order_project_1.controllers;

import com.example.order_project_1.models.entity.Orders;
import com.example.order_project_1.services.TestService;
import gaarason.database.contract.eloquent.Record;
import gaarason.database.contract.eloquent.RecordList;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TestController {

    @Resource
    TestService testService;

    @GetMapping("/")
    public String index() {
        testService.test();
        return "ok";
    }
}
