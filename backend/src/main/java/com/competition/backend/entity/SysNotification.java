package com.competition.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sys_notification")
public class SysNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 接收人ID，关联sys_user.id
     */
    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    /**
     * 通知类型：APPLY_RECEIVED / APPLY_APPROVED / APPLY_REJECTED /
     * TEAM_INVITE / AUDIT_SUBMITTED / AUDIT_APPROVED / AUDIT_REJECTED /
     * RESUBMIT_REQUIRED / AWARD_SUBMITTED / AWARD_APPROVED / AWARD_REJECTED
     */
    @Column(name = "type", nullable = false, length = 32)
    private String type;

    @Column(name = "title", nullable = false, length = 128)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 关联业务ID，前端可跳转到对应页面
     */
    @Column(name = "related_id")
    private Long relatedId;

    /**
     * 是否已读，默认FALSE
     */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    /**
     * 通知只有已读状态变更，无需 updated_at
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}