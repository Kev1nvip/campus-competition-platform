package com.competition.backend.repository;

import com.competition.backend.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {
    // 查询某学生作为队长的队伍
    List<Team> findByLeaderId(Long leaderId);
    // 查询某竞赛下的所有队伍（分页）
    Page<Team> findByCompetitionId(Long competitionId, Pageable pageable);
}