package com.competition.backend.repository;

import com.competition.backend.entity.ApplyRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface ApplyRecordRepository extends JpaRepository<ApplyRecord, Long> {
    Optional<ApplyRecord> findByTypeAndBizId(String type, Long bizId);
    List<ApplyRecord> findByReceiverId(Long receiverId);
    List<ApplyRecord> findByApplicantId(Long applicantId);
    // 查询老师收到的待处理申请（分页）
    Page<ApplyRecord> findByReceiverIdAndTypeAndStatus(Long receiverId, String type, String status, Pageable pageable);
    // 按多种类型查询
    Page<ApplyRecord> findByReceiverIdAndTypeIn(Long receiverId, List<String> types, Pageable pageable);
}