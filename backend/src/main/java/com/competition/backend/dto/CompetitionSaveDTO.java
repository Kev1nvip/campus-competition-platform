package com.competition.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class CompetitionSaveDTO {

    @NotBlank(message = "竞赛名称不能为空")
    private String title;

    @NotBlank(message = "竞赛类型不能为空")
    private String type; // INDIVIDUAL / TEAM

    @NotBlank(message = "主办方不能为空")
    private String organizer;

    private String requirement;

    @NotNull(message = "报名开始时间不能为空")
    private OffsetDateTime signupStart;

    @NotNull(message = "报名截止时间不能为空")
    private OffsetDateTime signupEnd;

    private OffsetDateTime competitionStart;

    private OffsetDateTime competitionEnd;

    @NotNull(message = "是否有名额限制不能为空")
    private Boolean hasQuota;

    @Min(value = 1, message = "名额上限最小为1")
    private Integer maxQuota;

    private Integer minTeamSize;

    private Integer maxTeamSize;

    private Integer maxTeachQuota;

    private String description;

    private String attachmentUrl;
}