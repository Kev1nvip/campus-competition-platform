package com.competition.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class CreateTeacherRecruitmentDTO {

    @NotNull
    private Long competitionId;

    @NotNull
    private Integer recruitCount;

    private String requirement;

    private OffsetDateTime deadline;
}