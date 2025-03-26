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
@Table(name = "users")
public class Users extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** auto generator start **/


    @Column(name = "username", length = 50L)
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "studentIdOrEmployeeId", length = 20L)
    private String studentIdOrEmployeeId;

    @Column(name = "role", length = 5L)
    private String role;

    @Column(name = "createdAt")
    private LocalDateTime createdat;

    @Column(name = "specialty", nullable = true)
    private String specialty;

    @Column(name = "contactInfo", nullable = true)
    private String contactinfo;

    @Column(name = "avatar", nullable = true)
    private String avatar;

    @Column(name = "user_status", nullable = true, length = 3L)
    private String user_status;

    @Column(name = "total_performance")
    private Double total_performance;
    /** auto generator end **/

    @Repository
    public static class Model extends BaseEntity.BaseModel<Users, Long> {

    }

}