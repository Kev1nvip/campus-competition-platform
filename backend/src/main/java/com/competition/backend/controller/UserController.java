package com.competition.backend.controller;

import com.competition.backend.common.result.PageVO;
import com.competition.backend.common.result.Result;
import com.competition.backend.util.SecurityUtil;
import com.competition.backend.dto.UserUpdateDTO;
import com.competition.backend.entity.AwardRecord;
import com.competition.backend.repository.AwardRecordRepository;
import com.competition.backend.repository.CompetitionRepository;
import com.competition.backend.repository.IndividualSignupRepository;
import com.competition.backend.repository.SysUserRepository;
import com.competition.backend.repository.TeamSignupRepository;
import com.competition.backend.service.UserService;
import com.competition.backend.vo.TeacherProfileVO;
import com.competition.backend.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "用户模块")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final IndividualSignupRepository individualSignupRepository;
    private final TeamSignupRepository teamSignupRepository;
    private final AwardRecordRepository awardRecordRepository;
    private final CompetitionRepository competitionRepository;
    private final SysUserRepository sysUserRepository;

    @Operation(summary = "获取当前登录用户的个人信息")
    @GetMapping("/info")
    public Result<UserInfoVO> getUserInfo() {
        Long userId = SecurityUtil.getCurrentUserId();
        UserInfoVO vo = userService.getUserInfo(userId);
        return Result.success(vo);
    }

    @Operation(summary = "更新当前登录用户的个人信息")
    @PutMapping("/info")
    public Result<Void> updateUserInfo(@Validated @RequestBody UserUpdateDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        userService.updateUserInfo(userId, dto);
        return Result.success();
    }

    @Operation(summary = "获取教师主页公开信息")
    @GetMapping("/teacher/{teacherId}")
    public Result<TeacherProfileVO> getTeacherProfile(
            @Parameter(description = "教师用户ID") @PathVariable Long teacherId) {
        TeacherProfileVO vo = userService.getTeacherProfile(teacherId);
        return Result.success(vo);
    }

    @Operation(summary = "教师带队统计（竞赛数、获奖数、获奖明细）")
    @GetMapping("/teacher-stats")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Map<String, Object>> teacherStats() {
        Long teacherId = SecurityUtil.getCurrentUserId();

        // 统计带队竞赛数（个人赛 + 团队赛，去重）
        Set<Long> compIds = new LinkedHashSet<>();
        individualSignupRepository.findByTeacherIdAndStatus(teacherId, "APPROVED")
                .forEach(s -> compIds.add(s.getCompetitionId()));
        teamSignupRepository.findByTeacherIdAndStatus(teacherId, "APPROVED")
                .forEach(s -> compIds.add(s.getCompetitionId()));

        // 统计获奖记录（老师提交的获奖记录）
        List<AwardRecord> awards = awardRecordRepository.findBySubmitterId(teacherId,
                PageRequest.of(0, 200)).getContent();

        long approvedCount = awards.stream().filter(a -> "APPROVED".equals(a.getStatus())).count();

        List<Map<String, Object>> awardList = awards.stream().map(a -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", a.getId());
            item.put("awardName", a.getAwardName());
            item.put("awardLevel", a.getAwardLevel());
            item.put("awardDate", a.getAwardDate());
            item.put("status", a.getStatus());
            competitionRepository.findById(a.getCompetitionId())
                    .ifPresent(c -> item.put("competitionTitle", c.getTitle()));
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("totalCompetitions", compIds.size());
        result.put("approvedAwards", approvedCount);
        result.put("awardList", awardList);
        return Result.success(result);
    }

    @Operation(summary = "管理员分页查询用户列表")
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageVO<UserInfoVO>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return Result.success(userService.listUsers(page, size, keyword));
    }

    @Operation(summary = "教师列表（学生报名选老师用，无权限限制）")
    @GetMapping("/teachers")
    public Result<PageVO<TeacherProfileVO>> listTeachers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String keyword) {
        return Result.success(userService.listTeachers(page, size, keyword));
    }

    @Operation(summary = "管理员禁用或启用用户")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> toggleStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        userService.toggleUserStatus(id, status);
        return Result.success();
    }
}
