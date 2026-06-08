package com.competition.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateTeamDTO {

    @NotNull
    private Long competitionId;

    @NotBlank
    private String teamName;
}