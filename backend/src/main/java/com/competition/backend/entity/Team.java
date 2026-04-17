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
@Table(name = "team")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联竞赛ID，关联competition.id
     */
    @Column(name = "competition_id", nullable = false)
    private Long competitionId;

    @Column(name = "team_name", nullable = false, length = 64)
    private String teamName;

    /**
     * 队长ID，关联sys_user.id
     */
    @Column(name = "leader_id", nullable = false)
    private Long leaderId;

    /**
     * 指导老师ID，关联sys_user.id，可为NULL
     */
    @Column(name = "teacher_id")
    private Long teacherId;

    /**
     * 老师是否已确认带队
     * TRUE后才能发组队招募帖
     */
    @Column(name = "teacher_confirmed", nullable = false)
    private Boolean teacherConfirmed;

    /**
     * 当前成员数量，含队长
     */
    @Column(name = "member_count", nullable = false)
    private Integer memberCount;

    /**
     * 队伍状态：FORMING / FULL / SUBMITTED / APPROVED / REJECTED / DISMISSED
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