package com.competition.backend.repository;

import com.competition.backend.entity.TeamRecruitment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRecruitmentRepository extends JpaRepository<TeamRecruitment, Long> {
    Page<TeamRecruitment> findByCompetitionIdAndStatus(Long competitionId, String status, Pageable pageable);
    Page<TeamRecruitment> findByLeaderId(Long leaderId, Pageable pageable);
}
