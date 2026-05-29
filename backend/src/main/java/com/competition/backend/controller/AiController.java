package com.competition.backend.controller;

import com.competition.backend.common.result.Result;
import com.competition.backend.dto.AiRecommendRequest;
import com.competition.backend.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 推荐模块")
//@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerToken")
public class AiController {

    private final AiService aiService;

    @Operation(summary = "竞赛智能推荐")
    @PostMapping("/recommend")
    public Result<String> recommend(@Validated @RequestBody AiRecommendRequest request) {
        return Result.success(aiService.recommend(request.getPrompt()));
    }

    @Operation(summary = "手动刷新 AI 知识库")
    @PostMapping("/knowledge/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> refresh() {
        aiService.triggerKnowledgeRefresh();
        return Result.success("知识库刷新任务已启动");
    }
}
