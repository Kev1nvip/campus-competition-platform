package com.competition.backend.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AwardVO {

    private Long id;
    private Long competitionId;
    private Long submitterId;
    private String submitterName; // 提交人姓名（可选，方便前端显示）
    private String bizType;
    private Long bizId;
    private String awardLevel;
    private String awardName;
    private String certificateUrl;
    private LocalDate awardDate;
    private String status;
    private OffsetDateTime createdAt;
}