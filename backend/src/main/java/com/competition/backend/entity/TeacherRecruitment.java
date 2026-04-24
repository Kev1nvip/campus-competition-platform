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
@Table(name = "teacher_recruitment")
public class TeacherRecruitment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联竞赛ID，关联competition.id
     */
    @Column(name = "competition_id", nullable = false)
    private Long competitionId;

    /**
     * 发布老师ID，关联sys_user.id
     */
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    /**
     * 招募人数上限
     */
    @Column(name = "recruit_count", nullable = false)
    private Integer recruitCount;

    /**
     * 当前已加入人数
     */
    @Column(name = "current_count", nullable = false)
    private Integer currentCount;

    @Column(name = "requirement", columnDefinition = "TEXT")
    private String requirement;

    @Column(name = "deadline")
    private OffsetDateTime deadline;

    /**
     * 状态：OPEN / FULL / CLOSED
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