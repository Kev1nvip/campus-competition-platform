package com.competition.backend.service;

import com.competition.backend.dto.SignupAuditDTO;

public interface AuditService {
    void auditSignup(Long adminId, SignupAuditDTO dto);
}