package com.competition.backend.controller;

import com.competition.backend.common.result.PageVO;
import com.competition.backend.common.result.Result;
import com.competition.backend.dto.ApplyRecruitmentDTO;
import com.competition.backend.dto.AuditApplyDTO;
import com.competition.backend.dto.CreateTeacherRecruitmentDTO;
import com.competition.backend.entity.TeamRecruitment;
import com.competition.backend.repository.TeamRepository;
import com.competition.backend.repository.TeamRecruitmentRepository;
import com.competition.backend.service.RecruitmentService;
import com.competition.backend.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "招募帖模块")
@RestController
@RequestMapping("/api/v1/recruitment")
@RequiredArgsConstructor
public class RecruitmentController {

    private final RecruitmentService recruitmentService;
    private final TeamRecruitmentRepository teamRecruitmentRepository;
    private final TeamRepository teamRepository;

    // ─── 老师招募帖 ─────────────────────────────

    @Operation(summary = "发布老师招募帖")
    @PostMapping("/teacher")
    public Result<Void> createTeacherRecruitment(
            @Validated @RequestBody CreateTeacherRecruitmentDTO dto) {
        Long teacherId = SecurityUtil.getCurrentUserId();
        recruitmentService.createTeacherRecruitment(teacherId, dto);
        return Result.success();
    }

    @Operation(summary = "关闭老师招募帖")
    @PutMapping("/teacher/{id}/close")
    public Result<Void> closeTeacherRecruitment(@PathVariable Long id) {
        Long teacherId = SecurityUtil.getCurrentUserId();
        recruitmentService.closeTeacherRecruitment(teacherId, id);
        return Result.success();
    }

    @Operation(summary = "学生申请加入老师招募")
    @PostMapping("/teacher/{id}/apply")
    public Result<Void> applyTeacherRecruitment(
            @PathVariable Long id,
            @RequestBody ApplyRecruitmentDTO dto) {
        Long studentId = SecurityUtil.getCurrentUserId();
        recruitmentService.applyTeacherRecruitment(studentId, id, dto);
        return Result.success();
    }

    @Operation(summary = "教师审核申请")
    @PutMapping("/apply/{applyId}/audit")
    public Result<Void> auditApply(
            @PathVariable Long applyId,
            @RequestBody AuditApplyDTO dto) {
        Long teacherId = SecurityUtil.getCurrentUserId();
        recruitmentService.auditApply(teacherId, applyId, dto);
        return Result.success();
    }

    // ─── 学生组队招募帖 ──────────────────────────

    @Operation(summary = "发布学生组队招募帖（队长）")
    @PostMapping("/team")
    public Result<Void> createTeamRecruitment(@RequestBody TeamRecruitment dto) {
        Long leaderId = SecurityUtil.getCurrentUserId();
        // 校验队伍是否存在且当前用户是队长
        teamRepository.findById(dto.getTeamId()).ifPresent(team -> {
            if (!team.getLeaderId().equals(leaderId)) {
                throw new com.competition.backend.common.exception.BusinessException(
                        com.competition.backend.common.constant.ErrorCode.FORBIDDEN, "只有队长可以发布招募帖");
            }
            if (!Boolean.TRUE.equals(team.getTeacherConfirmed())) {
                throw new com.competition.backend.common.exception.BusinessException(
                        com.competition.backend.common.constant.ErrorCode.TEAM_TEACHER_NOT_CONFIRMED, "指导老师尚未确认，不能发招募帖");
            }
        });

        TeamRecruitment recruitment = TeamRecruitment.builder()
                .competitionId(dto.getCompetitionId())
                .teamId(dto.getTeamId())
                .leaderId(leaderId)
                .recruitCount(dto.getRecruitCount())
                .currentCount(0)
                .requirement(dto.getRequirement())
                .deadline(dto.getDeadline())
                .status("OPEN")
                .build();
        teamRecruitmentRepository.save(recruitment);
        return Result.success();
    }

    @Operation(summary = "查看组队招募帖列表（按竞赛）")
    @GetMapping("/team")
    public Result<PageVO<TeamRecruitment>> listTeamRecruitments(
            @RequestParam Long competitionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageResult = teamRecruitmentRepository.findByCompetitionIdAndStatus(
                competitionId, "OPEN",
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return Result.success(PageVO.of(pageResult));
    }

    @Operation(summary = "关闭组队招募帖（队长）")
    @PutMapping("/team/{id}/close")
    public Result<Void> closeTeamRecruitment(@PathVariable Long id) {
        Long leaderId = SecurityUtil.getCurrentUserId();
        TeamRecruitment r = teamRecruitmentRepository.findById(id)
                .orElseThrow(() -> new com.competition.backend.common.exception.BusinessException(
                        com.competition.backend.common.constant.ErrorCode.NOT_FOUND, "招募帖不存在"));
        if (!r.getLeaderId().equals(leaderId)) {
            throw new com.competition.backend.common.exception.BusinessException(
                    com.competition.backend.common.constant.ErrorCode.FORBIDDEN, "只有队长可关闭");
        }
        r.setStatus("CLOSED");
        teamRecruitmentRepository.save(r);
        return Result.success();
    }
}