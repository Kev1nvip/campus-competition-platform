package com.competition.backend.repository;

import com.competition.backend.entity.SignupAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignupAuditRepository extends JpaRepository<SignupAudit, Long> {
}