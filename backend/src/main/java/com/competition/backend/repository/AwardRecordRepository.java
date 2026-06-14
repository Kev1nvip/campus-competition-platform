package com.competition.backend.repository;

import com.competition.backend.entity.AwardRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AwardRecordRepository extends JpaRepository<AwardRecord, Long> {
    Page<AwardRecord> findByCompetitionId(Long competitionId, Pageable pageable);
    Page<AwardRecord> findBySubmitterId(Long submitterId, Pageable pageable);
    long countByStatus(String status);
    Page<AwardRecord> findByStatus(String status, Pageable pageable);
}