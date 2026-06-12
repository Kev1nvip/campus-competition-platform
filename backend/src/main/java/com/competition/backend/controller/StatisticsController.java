package com.competition.backend.controller;

import com.competition.backend.common.result.Result;
import com.competition.backend.service.StatisticsService;
import com.competition.backend.vo.StatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "数据统计模块")
@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "获取首页综合数据看板")
    @GetMapping("/dashboard")
    public Result<StatisticsVO> getDashboardStatistics() {
        return Result.success(statisticsService.getDashboardStatistics());
    }
}