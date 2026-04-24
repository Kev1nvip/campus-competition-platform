package com.competition.backend.vo;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class CompetitionListVO {
    private Long competitionId;
    private String title;
    private String type;
    private String organizer;
    private String status;
    private OffsetDateTime signupStart;
    private OffsetDateTime signupEnd;
    private Boolean hasQuota;
    private Integer maxQuota;
    private Integer remainingQuota;
    private Long createdBy;
    private String createdByName;
    private OffsetDateTime createdAt;
}