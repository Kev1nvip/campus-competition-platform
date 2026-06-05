package com.competition.backend.controller;

import com.competition.backend.common.result.Result;
import com.competition.backend.dto.ApplyRecruitmentDTO;
import com.competition.backend.dto.AuditApplyDTO;
import com.competition.backend.dto.CreateTeacherRecruitmentDTO;
import com.competition.backend.service.RecruitmentService;
import com.competition.backend.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "招募帖模块")
@RestController
@RequestMapping("/api/v1/recruitment")
@RequiredArgsConstructor
public class RecruitmentController {

    private final RecruitmentService recruitmentService;

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
}