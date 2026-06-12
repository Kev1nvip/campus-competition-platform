package com.competition.backend.dto;

import lombok.Data;

@Data
public class AuditApplyDTO {

    /**
     * APPROVED / REJECTED
     */
    private String status;

    private String reason;
}