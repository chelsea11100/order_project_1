package com.example.order_project_1.models.entity;

import cn.hutool.json.JSONObject;
import com.example.order_project_1.models.entity.base.BaseEntity;
import gaarason.database.annotation.Column;
import gaarason.database.annotation.Table;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Repository;


@Getter
@Data
@ToString(callSuper = true)
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Table(name = "performance_records")
public class PerformanceRecords extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** auto generator start **/


    @Column(name = "staff_id")
    private Long staffId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "workload")
    private String workload;

    @Column(name = "salary")
    private Double salary;

    @Column(name = "createdAt")
    private LocalDateTime createdat;

    @Column(name = "degree")
    private double degree;
    @Override
    public String toString() {
        return "PerformanceRecords{" +
                "staffId=" + staffId +
                ", createdat=" + createdat +
                ", orderId=" + orderId +
                ", workload=" + workload +
                ", degree=" + degree +
                ", salary=" + salary +
                '}';
    }



    /** auto generator end **/

    @Repository
    public static class Model extends BaseEntity.BaseModel<PerformanceRecords, Long> {

    }

}