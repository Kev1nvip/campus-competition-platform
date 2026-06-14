package com.competition.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "award_record")
public class AwardRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 竞赛ID，关联competition.id
     */
    @Column(name = "competition_id", nullable = false)
    private Long competitionId;

    /**
     * 提交人ID，关联sys_user.id
     */
    @Column(name = "submitter_id", nullable = false)
    private Long submitterId;

    /**
     * 报名类型：INDIVIDUAL / TEAM
     */
    @Column(name = "biz_type", nullable = false, length = 16)
    private String bizType;

    /**
     * INDIVIDUAL→individual_signup.id，TEAM→team_signup.id
     */
    @Column(name = "biz_id", nullable = false)
    private Long bizId;

    /**
     * 奖项等级：NATIONAL_FIRST / NATIONAL_SECOND / NATIONAL_THIRD /
     * PROVINCIAL_FIRST / PROVINCIAL_SECOND / PROVINCIAL_THIRD / OTHER
     */
    @Column(name = "award_level", nullable = false, length = 32)
    private String awardLevel;

    @Column(name = "award_name", nullable = false, length = 128)
    private String awardName;

    /**
     * 证书图片相对路径
     */
    @Column(name = "certificate_url", nullable = false, length = 512)
    private String certificateUrl;

    @Column(name = "award_date", nullable = false)
    private LocalDate awardDate;

    /**
     * 状态：PENDING / APPROVED / REJECTED
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