package com.competition.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IndividualSignupDTO {
    @NotNull(message = "竞赛ID不能为空")
    private Long competitionId;
    @NotNull(message = "指导老师不能为空")
    private Long teacherId;
    private String motivation;
    private String introduction;
}