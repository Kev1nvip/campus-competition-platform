package com.competition.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SignupAuditDTO {

    @NotBlank(message = "业务类型不能为空")
    private String bizType; // 传 "INDIVIDUAL" 或 "TEAM"

    @NotNull(message = "业务ID不能为空")
    private Long bizId; // 报名表的ID

    @NotBlank(message = "审核结果不能为空")
    private String result; // 传 "APPROVED" 或 "REJECTED"

    // 如果驳回，建议填原因
    private String rejectReason;
}