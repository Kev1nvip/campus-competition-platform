package com.competition.backend.controller;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.common.result.PageVO;
import com.competition.backend.common.result.Result;
import com.competition.backend.dto.ApplyRecruitmentDTO;
import com.competition.backend.dto.AuditApplyDTO;
import com.competition.backend.dto.CreateTeacherRecruitmentDTO;
import com.competition.backend.entity.ApplyRecord;
import com.competition.backend.entity.IndividualSignup;
import com.competition.backend.entity.TeamRecruitment;
import com.competition.backend.repository.ApplyRecordRepository;
import com.competition.backend.repository.CompetitionRepository;
import com.competition.backend.repository.IndividualSignupRepository;
import com.competition.backend.repository.SysUserRepository;
import com.competition.backend.repository.TeamRepository;
import com.competition.backend.repository.TeamRecruitmentRepository;
import com.competition.backend.service.NotificationService;
import com.competition.backend.service.RecruitmentService;
import com.competition.backend.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "招募帖模块")
@RestController
@RequestMapping("/api/v1/recruitment")
@RequiredArgsConstructor
public class RecruitmentController {

    private final RecruitmentService recruitmentService;
    private final TeamRecruitmentRepository teamRecruitmentRepository;
    private final TeamRepository teamRepository;
    private final ApplyRecordRepository applyRecordRepository;
    private final IndividualSignupRepository individualSignupRepository;
    private final SysUserRepository userRepository;
    private final CompetitionRepository competitionRepository;
    private final NotificationService notificationService;

    // ─── 个人赛指导申请（INDIVIDUAL_GUIDE）───────────

    @Operation(summary = "老师查看收到的个人赛指导申请列表（待处理）")
    @GetMapping("/guide/pending")
    public Result<List<Map<String, Object>>> pendingGuideApplies() {
        Long teacherId = SecurityUtil.getCurrentUserId();
        List<Map<String, Object>> result = applyRecordRepository.findByReceiverId(teacherId).stream()
                .filter(r -> "INDIVIDUAL_GUIDE".equals(r.getType()) && "PENDING".equals(r.getStatus()))
                .map(r -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("applyId", r.getId());
                    item.put("bizId", r.getBizId()); // individual_signup.id
                    item.put("applicantId", r.getApplicantId());
                    item.put("createdAt", r.getCreatedAt());
                    // 关联学生信息
                    userRepository.findById(r.getApplicantId()).ifPresent(u -> {
                        item.put("studentName", u.getRealName());
                        item.put("studentNo", u.getStudentNo());
                        item.put("department", u.getDepartment());
                    });
                    // 关联报名信息（竞赛名）
                    individualSignupRepository.findById(r.getBizId()).ifPresent(signup ->
                        competitionRepository.findById(signup.getCompetitionId())
                            .ifPresent(c -> item.put("competitionTitle", c.getTitle()))
                    );
                    return item;
                })
                .collect(Collectors.toList());
        return Result.success(result);
    }

    @Operation(summary = "老师处理个人赛指导申请（同意/拒绝）")
    @PutMapping("/guide/{applyId}/handle")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> handleGuideApply(
            @PathVariable Long applyId,
            @RequestParam String action) {
        Long teacherId = SecurityUtil.getCurrentUserId();
        ApplyRecord apply = applyRecordRepository.findById(applyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLY_NOT_FOUND, "申请不存在"));

        if (!apply.getReceiverId().equals(teacherId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限处理");
        }
        if (!"PENDING".equals(apply.getStatus())) {
            throw new BusinessException(ErrorCode.APPLY_ALREADY_HANDLED, "已处理");
        }
        if (!"INDIVIDUAL_GUIDE".equals(apply.getType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "申请类型不匹配");
        }

        apply.setStatus(action);
        applyRecordRepository.save(apply);

        if ("APPROVED".equals(action)) {
            // 报名状态变为 DRAFT（等待学生提交管理员审核）
            individualSignupRepository.findById(apply.getBizId()).ifPresent(signup -> {
                signup.setStatus("DRAFT");
                individualSignupRepository.save(signup);
            });
            // 通知学生
            String teacherName = userRepository.findById(teacherId).map(u -> u.getRealName()).orElse("老师");
            notificationService.send(apply.getApplicantId(), "APPLY_APPROVED", "指导申请已通过",
                    "「" + teacherName + "」同意指导你参赛，请前往「我的报名」提交管理员审核",
                    apply.getBizId());
        } else if ("REJECTED".equals(action)) {
            // 通知学生
            notificationService.send(apply.getApplicantId(), "APPLY_REJECTED", "指导申请被拒绝",
                    "你的指导申请被拒绝，请重新选择其他老师",
                    apply.getBizId());
        }
        return Result.success();
    }

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

    @Operation(summary = "教师审核招募帖申请")
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
        teamRepository.findById(dto.getTeamId()).ifPresent(team -> {
            if (!team.getLeaderId().equals(leaderId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "只有队长可以发布招募帖");
            }
            if (!Boolean.TRUE.equals(team.getTeacherConfirmed())) {
                throw new BusinessException(ErrorCode.TEAM_TEACHER_NOT_CONFIRMED, "指导老师尚未确认，不能发招募帖");
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
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "招募帖不存在"));
        if (!r.getLeaderId().equals(leaderId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有队长可关闭");
        }
        r.setStatus("CLOSED");
        teamRecruitmentRepository.save(r);
        return Result.success();
    }
}