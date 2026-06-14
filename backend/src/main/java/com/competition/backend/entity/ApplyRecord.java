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
@Table(name = "apply_record")
public class ApplyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 申请类型：INDIVIDUAL_GUIDE / TEAM_GUIDE /
     * TEACHER_RECRUIT_APPLY / TEAM_RECRUIT_APPLY / TEAM_INVITE
     */
    @Column(name = "type", nullable = false, length = 32)
    private String type;

    /**
     * 申请发起人ID，关联sys_user.id
     */
    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    /**
     * 申请接收人ID，关联sys_user.id
     */
    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    /**
     * 关联业务ID，含义由type决定
     */
    @Column(name = "biz_id", nullable = false)
    private Long bizId;

    @Column(name = "introduction", columnDefinition = "TEXT")
    private String introduction;

    @Column(name = "motivation", columnDefinition = "TEXT")
    private String motivation;

    /**
     * 申请状态：PENDING / APPROVED / REJECTED
     */
    @Column(name = "status", nullable = false, length = 16)
    private String status;

    /**
     * 拒绝原因
     */
    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}