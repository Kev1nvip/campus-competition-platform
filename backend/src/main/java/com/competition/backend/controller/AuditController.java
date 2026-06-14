package com.competition.backend.controller;

import com.competition.backend.common.result.Result;
import com.competition.backend.dto.SignupAuditDTO;
import com.competition.backend.service.AuditService;
import com.competition.backend.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "审核模块")
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @Operation(summary = "管理员审核报名记录")
    @PostMapping("/signup")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> auditSignup(@Valid @RequestBody SignupAuditDTO dto) {
        // 获取当前登录的管理员ID
        Long adminId = SecurityUtil.getCurrentUserId();
        auditService.auditSignup(adminId, dto);
        return Result.success();
    }
}