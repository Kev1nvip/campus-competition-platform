package com.competition.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sys_user")
public class SysUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 64, unique = true)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "real_name", nullable = false, length = 32)
    private String realName;

    /**
     * 角色：STUDENT / TEACHER / ADMIN
     */
    @Column(name = "role", nullable = false, length = 16)
    private String role;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 128)
    private String email;

    /**
     * 学号，学生必填，全局唯一
     */
    @Column(name = "student_no", length = 32, unique = true)
    private String studentNo;

    @Column(name = "department", length = 64)
    private String department;

    /**
     * 职称，老师专属，如讲师/副教授/教授
     */
    @Column(name = "title", length = 32)
    private String title;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    /**
     * 账号状态：ACTIVE / DISABLED
     */
    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}