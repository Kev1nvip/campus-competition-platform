package com.competition.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiRecommendRequest {

    @NotBlank(message = "prompt不能为空")
    @Size(max = 1000, message = "prompt长度不能超过1000字符")
    private String prompt;
}
