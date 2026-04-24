package com.competition.backend.controller;

import com.competition.backend.common.result.PageVO;
import com.competition.backend.common.result.Result;
import com.competition.backend.dto.CompetitionSaveDTO;
import com.competition.backend.entity.Competition;
import com.competition.backend.service.CompetitionService;
import com.competition.backend.vo.CompetitionListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "竞赛模块")
@RestController
@RequestMapping("/api/v1/competitions")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerToken") 
public class CompetitionController {

    private final CompetitionService competitionService;

    @Operation(summary = "发布竞赛")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Long> create(@Validated @RequestBody CompetitionSaveDTO saveDTO) {
        return Result.success(competitionService.createCompetition(saveDTO));
    }

    @Operation(summary = "竞赛列表")
    @GetMapping
    public Result<PageVO<CompetitionListVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        return Result.success(competitionService.getCompetitionList(page, size, status, type, keyword));
    }

    @Operation(summary = "竞赛详情")
    @GetMapping("/{id}")
    public Result<Competition> detail(@PathVariable Long id) {
        return Result.success(competitionService.getCompetitionDetail(id));
    }

    @Operation(summary = "编辑竞赛")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody CompetitionSaveDTO saveDTO) {
        competitionService.updateCompetition(id, saveDTO);
        return Result.success();
    }

    @Operation(summary = "变更竞赛状态")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        competitionService.changeStatus(id, body.get("action"));
        return Result.success();
    }
}