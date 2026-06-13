package com.competition.backend.controller;

import com.competition.backend.common.result.PageVO;
import com.competition.backend.common.result.Result;
import com.competition.backend.dto.IndividualSignupDTO;
import com.competition.backend.dto.SignupSubmitDTO;
import com.competition.backend.entity.IndividualSignup;
import com.competition.backend.repository.CompetitionRepository;
import com.competition.backend.repository.IndividualSignupRepository;
import com.competition.backend.repository.SysUserRepository;
import com.competition.backend.repository.TeamRepository;
import com.competition.backend.repository.TeamSignupRepository;
import com.competition.backend.service.SignupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "报名模块")
@RestController
@RequestMapping("/api/v1/signups")
@RequiredArgsConstructor
public class SignupController {

    private final SignupService signupService;
    private final IndividualSignupRepository individualSignupRepository;
    private final SysUserRepository userRepository;
    private final CompetitionRepository competitionRepository;
    private final TeamSignupRepository teamSignupRepository;
    private final TeamRepository teamRepository;

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

    @Operation(summary = "管理员查询待审核个人赛报名列表（含关联信息）")
    @GetMapping("/individual/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageVO<Map<String, Object>>> adminPendingList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<IndividualSignup> pageResult = individualSignupRepository.findByStatusIn(
                List.of("PENDING", "RESUBMITTED"),
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "submittedAt")));

        return Result.success(PageVO.of(pageResult, signup -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", signup.getId());
            item.put("competitionId", signup.getCompetitionId());
            item.put("studentId", signup.getStudentId());
            item.put("teacherId", signup.getTeacherId());
            item.put("status", signup.getStatus());
            item.put("submittedAt", signup.getSubmittedAt());
            item.put("rejectReason", signup.getRejectReason());
            // 关联学生信息
            userRepository.findById(signup.getStudentId()).ifPresent(u -> {
                item.put("studentName", u.getRealName());
                item.put("studentNo", u.getStudentNo());
                item.put("department", u.getDepartment());
            });
            // 关联竞赛信息
            competitionRepository.findById(signup.getCompetitionId()).ifPresent(c ->
                    item.put("competitionTitle", c.getTitle()));
            // 关联老师信息
            userRepository.findById(signup.getTeacherId()).ifPresent(u ->
                    item.put("teacherName", u.getRealName()));
            return item;
        }));
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

    @Operation(summary = "管理员查询待审核团队赛报名列表（含关联信息）")
    @GetMapping("/team/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageVO<Map<String, Object>>> adminTeamPendingList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<com.competition.backend.entity.TeamSignup> pageResult = teamSignupRepository.findByStatusIn(
                List.of("PENDING", "RESUBMITTED"),
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "submittedAt")));

        return Result.success(PageVO.of(pageResult, signup -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", signup.getId());
            item.put("competitionId", signup.getCompetitionId());
            item.put("teamId", signup.getTeamId());
            item.put("teacherId", signup.getTeacherId());
            item.put("status", signup.getStatus());
            item.put("submittedAt", signup.getSubmittedAt());
            item.put("rejectReason", signup.getRejectReason());
            // 关联竞赛
            competitionRepository.findById(signup.getCompetitionId())
                    .ifPresent(c -> item.put("competitionTitle", c.getTitle()));
            // 关联队伍和队长
            teamRepository.findById(signup.getTeamId()).ifPresent(t -> {
                item.put("teamName", t.getTeamName());
                item.put("memberCount", t.getMemberCount());
                userRepository.findById(t.getLeaderId())
                        .ifPresent(u -> item.put("leaderName", u.getRealName()));
            });
            // 关联老师
            if (signup.getTeacherId() != null) {
                userRepository.findById(signup.getTeacherId())
                        .ifPresent(u -> item.put("teacherName", u.getRealName()));
            }
            return item;
        }));
    }
}