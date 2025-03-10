package com.example.order_project_1.models.entity;

import com.example.order_project_1.models.entity.base.BaseEntity;
import gaarason.database.annotation.Column;
import gaarason.database.annotation.Table;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Repository;


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
    private String salary;

    @Column(name = "createdAt")
    private LocalDateTime createdat;


    /** auto generator end **/

    @Repository
    public static class Model extends BaseEntity.BaseModel<PerformanceRecords, Long> {

    }

}