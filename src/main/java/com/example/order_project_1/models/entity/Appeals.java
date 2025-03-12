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
@Table(name = "appeal")
public class Appeals extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** auto generator start **/


    @Column(name = "id")
    private Long id;

    @Column(name = "staff_id")
    private Long staffId;

    @Column(name = "performance_record_id")
    private Long performanceRecordId;

    @Column(name = "reason")
    private String reason;

    @Column(name = "status")
    private String status; // 'PENDING', 'APPROVED', 'REJECTED'

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;



    /** auto generator end **/

    @Repository
    public static class Model extends BaseEntity.BaseModel<Appeals, Long> {

    }

}
