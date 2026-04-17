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
@Table(name = "award_audit")
public class AwardAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联获奖记录ID，关联award_record.id
     */
    @Column(name = "award_record_id", nullable = false)
    private Long awardRecordId;

    /**
     * 审核人ID，关联sys_user.id
     */
    @Column(name = "auditor_id", nullable = false)
    private Long auditorId;

    /**
     * 审核结果：APPROVED / REJECTED
     */
    @Column(name = "result", nullable = false, length = 16)
    private String result;

    /**
     * 驳回原因
     */
    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    /**
     * 审核记录不可修改，无需 updated_at
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}