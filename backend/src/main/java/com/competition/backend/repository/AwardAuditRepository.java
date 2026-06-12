package com.competition.backend.repository;

import com.competition.backend.entity.AwardAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AwardAuditRepository extends JpaRepository<AwardAudit, Long> {
}