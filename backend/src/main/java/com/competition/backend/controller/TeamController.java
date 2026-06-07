package com.competition.backend.controller;

import com.competition.backend.common.result.Result;
import com.competition.backend.dto.CreateTeamDTO;
import com.competition.backend.service.TeamService;
import com.competition.backend.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "组队模块")
@RestController
@RequestMapping("/api/v1/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @Operation(summary = "创建队伍")
    @PostMapping
    public Result<Void> createTeam(@RequestBody CreateTeamDTO dto) {
        teamService.createTeam(SecurityUtil.getCurrentUserId(), dto);
        return Result.success();
    }

    @Operation(summary = "邀请队友")
    @PostMapping("/{id}/invite")
    public Result<Void> invite(@PathVariable Long id,
                               @RequestParam Long targetUserId) {
        teamService.inviteMember(SecurityUtil.getCurrentUserId(), id, targetUserId);
        return Result.success();
    }

    @Operation(summary = "处理邀请")
    @PutMapping("/invite/{applyId}")
    public Result<Void> handleInvite(@PathVariable Long applyId,
                                     @RequestParam String status) {
        teamService.handleInvite(SecurityUtil.getCurrentUserId(), applyId, status);
        return Result.success();
    }

    @Operation(summary = "退出队伍")
    @DeleteMapping("/{id}/quit")
    public Result<Void> quit(@PathVariable Long id) {
        teamService.quitTeam(SecurityUtil.getCurrentUserId(), id);
        return Result.success();
    }
}