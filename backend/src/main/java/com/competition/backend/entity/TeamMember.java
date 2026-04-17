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
@Table(name = "team_member")
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 队伍ID，关联team.id
     */
    @Column(name = "team_id", nullable = false)
    private Long teamId;

    /**
     * 学生ID，关联sys_user.id
     */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /**
     * 成员角色：LEADER / MEMBER
     */
    @Column(name = "role", nullable = false, length = 16)
    private String role;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}