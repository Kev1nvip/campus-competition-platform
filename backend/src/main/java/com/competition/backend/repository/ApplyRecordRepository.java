package com.competition.backend.repository;

import com.competition.backend.entity.ApplyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ApplyRecordRepository extends JpaRepository<ApplyRecord, Long> {
    Optional<ApplyRecord> findByTypeAndBizId(String type, Long bizId);
}