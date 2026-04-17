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
@Table(name = "team_signup")
public class TeamSignup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 竞赛ID，关联competition.id
     */
    @Column(name = "competition_id", nullable = false)
    private Long competitionId;

    /**
     * 队伍ID，关联team.id
     */
    @Column(name = "team_id", nullable = false)
    private Long teamId;

    /**
     * 指导老师ID，关联sys_user.id
     */
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    /**
     * 报名状态：DRAFT / PENDING / APPROVED / REJECTED / RESUBMITTED
     */
    @Column(name = "status", nullable = false, length = 16)
    private String status;

    /**
     * 驳回原因，管理员填写
     */
    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    /**
     * 提交管理员审核的时间
     */
    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}