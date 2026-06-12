package com.competition.backend.controller;

import com.competition.backend.common.result.Result;
import com.competition.backend.dto.AwardAuditDTO;
import com.competition.backend.dto.CreateAwardDTO;
import com.competition.backend.service.AwardService;
import com.competition.backend.util.SecurityUtil;
import com.competition.backend.vo.AwardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@Tag(name = "获奖模块")
@RestController
@RequestMapping("/api/v1/award")
@RequiredArgsConstructor
public class AwardController {

    private final AwardService awardService;

    @Operation(summary = "提交获奖记录")
    @PostMapping
    public Result<Void> createAward(@Valid @RequestBody CreateAwardDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        awardService.createAward(userId, dto);
        return Result.success();
    }

    @Operation(summary = "管理员审核获奖记录")
    @PostMapping("/audit")
    public Result<Void> auditAward(@Valid @RequestBody AwardAuditDTO dto) {
        Long adminId = SecurityUtil.getCurrentUserId();
        awardService.auditAward(adminId, dto);
        return Result.success();
    }

    @Operation(summary = "按竞赛ID查询获奖列表（管理员用）")
    @GetMapping("/competition/{competitionId}")
    public Result<Page<AwardVO>> getAwardsByCompetition(
            @PathVariable Long competitionId,
            Pageable pageable) {
        Page<AwardVO> page = awardService.getAwardsByCompetition(competitionId, pageable);
        return Result.success(page);
    }

    @Operation(summary = "查询我的获奖记录")
    @GetMapping("/my")
    public Result<Page<AwardVO>> getMyAwards(Pageable pageable) {
        Long userId = SecurityUtil.getCurrentUserId();
        Page<AwardVO> page = awardService.getMyAwards(userId, pageable);
        return Result.success(page);
    }
}