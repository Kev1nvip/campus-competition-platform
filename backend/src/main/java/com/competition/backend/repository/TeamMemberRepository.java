package com.competition.backend.repository;

import com.competition.backend.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByTeamId(Long teamId);
    List<TeamMember> findByStudentId(Long studentId);
    boolean existsByTeamIdAndStudentId(Long teamId, Long studentId);
}
