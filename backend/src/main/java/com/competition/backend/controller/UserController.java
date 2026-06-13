package com.competition.backend.controller;

import com.competition.backend.common.result.PageVO;
import com.competition.backend.common.result.Result;
import com.competition.backend.util.SecurityUtil;
import com.competition.backend.dto.UserUpdateDTO;
import com.competition.backend.service.UserService;
import com.competition.backend.vo.TeacherProfileVO;
import com.competition.backend.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户模块")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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
}