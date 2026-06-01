package com.competition.backend.repository;

import com.competition.backend.entity.ApplyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface ApplyRecordRepository extends JpaRepository<ApplyRecord, Long> {
    Optional<ApplyRecord> findByTypeAndBizId(String type, Long bizId);
    List<ApplyRecord> findByReceiverId(Long receiverId);
    List<ApplyRecord> findByApplicantId(Long applicantId);
}