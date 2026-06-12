package com.competition.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateAwardDTO {

    @NotNull(message = "竞赛ID不能为空")
    private Long competitionId;

    @NotBlank(message = "报名类型不能为空")
    private String bizType; // INDIVIDUAL 或 TEAM

    @NotNull(message = "业务ID不能为空")
    private Long bizId; // 对应的报名记录ID

    @NotBlank(message = "奖项等级不能为空")
    private String awardLevel; // NATIONAL_FIRST 等

    @NotBlank(message = "奖项名称不能为空")
    private String awardName;

    @NotBlank(message = "证书图片路径不能为空")
    private String certificateUrl;

    @NotNull(message = "获奖日期不能为空")
    private LocalDate awardDate;
}