package com.competition.backend.repository;

import com.competition.backend.entity.TeamSignup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamSignupRepository extends JpaRepository<TeamSignup, Long> {
    boolean existsByCompetitionIdAndTeamId(Long competitionId, Long teamId);
    // 通过队伍成员关联查询较为复杂，此处先简单实现
    Page<TeamSignup> findByCompetitionId(Long competitionId, Pageable pageable);
}