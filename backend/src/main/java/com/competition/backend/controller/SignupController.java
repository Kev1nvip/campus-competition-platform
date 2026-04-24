package com.competition.backend.controller;

import com.competition.backend.common.result.PageVO;
import com.competition.backend.common.result.Result;
import com.competition.backend.dto.IndividualSignupDTO;
import com.competition.backend.dto.SignupSubmitDTO;
import com.competition.backend.entity.IndividualSignup;
import com.competition.backend.service.SignupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "报名模块")
@RestController
@RequestMapping("/api/v1/signups")
@RequiredArgsConstructor
public class SignupController {

    private final SignupService signupService;

    @Operation(summary = "个人赛报名")
    @PostMapping("/individual")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> signUpIndividual(@Validated @RequestBody IndividualSignupDTO dto) {
        return Result.success(signupService.signUpIndividual(dto));
    }

    @Operation(summary = "提交个人赛审核")
    @PostMapping("/individual/{id}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Void> submitIndividual(@PathVariable Long id, @RequestBody(required = false) SignupSubmitDTO dto) {
        signupService.submitIndividual(id, dto);
        return Result.success();
    }

    @Operation(summary = "我的个人赛报名列表")
    @GetMapping("/individual/my")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<PageVO<?>> myIndividual(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        return Result.success(signupService.getMyIndividualSignups(page, size, status));
    }

    @Operation(summary = "创建团队赛报名草稿")
    @PostMapping("/team")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> signUpTeam(@RequestBody Map<String, Long> body) {
        return Result.success(signupService.signUpTeam(body.get("teamId")));
    }

    @Operation(summary = "提交团队赛审核")
    @PostMapping("/team/{id}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Void> submitTeam(@PathVariable Long id) {
        signupService.submitTeam(id);
        return Result.success();
    }
}