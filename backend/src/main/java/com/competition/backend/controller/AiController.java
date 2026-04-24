package com.competition.backend.controller;

import com.competition.backend.common.result.Result;
import com.competition.backend.service.AiService;
import com.competition.backend.service.impl.KnowledgeBaseServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "AI 推荐模块")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final KnowledgeBaseServiceImpl knowledgeBaseService;

    @Operation(summary = "竞赛智能推荐")
    @PostMapping("/recommend")
    public Result<String> recommend(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        return Result.success(aiService.recommend(prompt));
    }

    @Operation(summary = "手动刷新 AI 知识库")
    @PostMapping("/knowledge/refresh")
    public Result<String> refresh() {
        knowledgeBaseService.refreshKnowledgeBase();
        return Result.success("知识库刷新任务已启动");
    }
}