package com.example.order_project_1.DTO;


import com.example.order_project_1.models.entity.Orders;
import com.example.order_project_1.models.entity.PerformanceRecords;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderHistoryResponse {
    private List<Orders> orders;
    private List<PerformanceRecords> performanceRecords;
}
