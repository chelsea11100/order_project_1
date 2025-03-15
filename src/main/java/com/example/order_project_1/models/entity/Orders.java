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
@Table(name = "orders")
public class Orders extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** auto generator start **/


    @Column(name = "user_id")
    private Long userId;

    @Column(name = "staff_id", nullable = true)
    private Long staffId;

    @Column(name = "status", length = 3L)
    private String status;

    @Column(name = "deviceType", length = 100L)
    private String devicetype;

    @Column(name = "problemDescription", length = 65535L)
    private String problemdescription;

    @Column(name = "appointmentTime")
    private LocalDateTime appointment;

    @Column(name = "createdAt")
    private LocalDateTime create;

    @Column(name = "acceptedAt", nullable = true)
    private LocalDateTime accepted;

    @Column(name = "completedAt", nullable = true)
    private LocalDateTime completed;

    @Column(name = "result", nullable = true, length = 3L)
    private String result;

    @Column(name = "userFeedback", nullable = true, length = 65535L)
    private String userfeedback;

    @Column(name = "image", nullable = true)
    private String image;



    /** auto generator end **/

    @Repository
    public static class Model extends BaseEntity.BaseModel<Orders, Long> {

    }

}