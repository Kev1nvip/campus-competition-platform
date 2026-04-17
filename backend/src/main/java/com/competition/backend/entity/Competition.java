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
@Table(name = "competition")
public class Competition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 128)
    private String title;

    /**
     * 竞赛类型：INDIVIDUAL / TEAM
     */
    @Column(name = "type", nullable = false, length = 16)
    private String type;

    @Column(name = "organizer", nullable = false, length = 128)
    private String organizer;

    @Column(name = "requirement", columnDefinition = "TEXT")
    private String requirement;

    @Column(name = "signup_start", nullable = false)
    private OffsetDateTime signupStart;

    @Column(name = "signup_end", nullable = false)
    private OffsetDateTime signupEnd;

    @Column(name = "competition_start")
    private OffsetDateTime competitionStart;

    @Column(name = "competition_end")
    private OffsetDateTime competitionEnd;

    /**
     * 是否有名额限制
     */
    @Column(name = "has_quota", nullable = false)
    private Boolean hasQuota;

    /**
     * 名额上限，hasQuota为true时必填
     */
    @Column(name = "max_quota")
    private Integer maxQuota;

    /**
     * 已报名数量，由Redis计数器维护并定期同步
     */
    @Column(name = "enrolled_count", nullable = false)
    private Integer enrolledCount;

    /**
     * 最少队伍人数，团队赛必填
     */
    @Column(name = "min_team_size")
    private Integer minTeamSize;

    /**
     * 最多队伍人数，团队赛必填
     */
    @Column(name = "max_team_size")
    private Integer maxTeamSize;

    /**
     * 每位老师最多带队数，NULL表示不限制
     */
    @Column(name = "max_teach_quota")
    private Integer maxTeachQuota;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "attachment_url", length = 512)
    private String attachmentUrl;

    /**
     * 竞赛状态：UPCOMING / SIGNING / CLOSED / ONGOING / FINISHED / OFFLINE
     */
    @Column(name = "status", nullable = false, length = 16)
    private String status;

    /**
     * 发布人ID，关联sys_user.id
     */
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    /**
     * 乐观锁版本号
     */
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}