package com.competition.backend.repository;

import com.competition.backend.entity.TeamSignup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamSignupRepository extends JpaRepository<TeamSignup, Long> {
    boolean existsByCompetitionIdAndTeamId(Long competitionId, Long teamId);
    Page<TeamSignup> findByCompetitionId(Long competitionId, Pageable pageable);
    Page<TeamSignup> findByStatusIn(List<String> statuses, Pageable pageable);
    List<TeamSignup> findByTeacherIdAndStatus(Long teacherId, String status);
    List<TeamSignup> findByTeacherIdAndCompetitionIdAndStatus(Long teacherId, Long competitionId, String status);
}