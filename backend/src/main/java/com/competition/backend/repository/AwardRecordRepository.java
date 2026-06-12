package com.competition.backend.repository;

import com.competition.backend.entity.AwardRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AwardRecordRepository extends JpaRepository<AwardRecord, Long> {
    // 按竞赛ID分页查询获奖记录（管理员用）
    Page<AwardRecord> findByCompetitionId(Long competitionId, Pageable pageable);

    // 按提交人ID分页查询（学生/老师查看自己的获奖记录）
    Page<AwardRecord> findBySubmitterId(Long submitterId, Pageable pageable);
}