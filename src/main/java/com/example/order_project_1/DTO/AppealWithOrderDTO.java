package com.example.order_project_1.DTO;

import com.example.order_project_1.models.entity.Appeals;
import com.example.order_project_1.models.entity.Orders;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppealWithOrderDTO {
    private Appeals appeal;
    private Orders order; // 注意类名是Orders
}