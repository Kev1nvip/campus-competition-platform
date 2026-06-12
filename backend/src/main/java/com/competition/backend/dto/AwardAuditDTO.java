package com.competition.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AwardAuditDTO {

    @NotNull(message = "获奖记录ID不能为空")
    private Long awardRecordId;

    @NotBlank(message = "审核结果不能为空")
    private String result; // APPROVED 或 REJECTED

    private String rejectReason; // 驳回时填写原因
}