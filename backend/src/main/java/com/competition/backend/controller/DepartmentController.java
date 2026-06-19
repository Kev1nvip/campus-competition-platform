package com.competition.backend.controller;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.common.result.Result;
import com.competition.backend.entity.Department;
import com.competition.backend.entity.SysUser;
import com.competition.backend.repository.DepartmentRepository;
import com.competition.backend.repository.SysUserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "院系管理")
@RestController
@RequestMapping("/api/v1/department")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentRepository departmentRepository;
    private final SysUserRepository userRepository;

    @Operation(summary = "院系列表（含各院系用户数）")
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Map<String, Object>>> list() {
        List<Department> depts = departmentRepository.findAll();
        List<Map<String, Object>> result = depts.stream()
                .sorted(Comparator.comparing(Department::getName))
                .map(d -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", d.getId());
                    item.put("name", d.getName());
                    item.put("createdAt", d.getCreatedAt());
                    item.put("userCount", userRepository.countByDepartment(d.getName()));
                    return item;
                })
                .collect(Collectors.toList());
        return Result.success(result);
    }

    @Operation(summary = "新增院系")
    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> add(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.PARAM_NULL, "院系名称不能为空");
        }
        name = name.trim();
        if (name.length() > 64) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "院系名称不能超过64位");
        }
        if (departmentRepository.existsByName(name)) {
            throw new BusinessException(ErrorCode.CONFLICT, "院系已存在");
        }
        departmentRepository.save(Department.builder().name(name).build());
        return Result.success();
    }

    @Operation(summary = "重命名院系（更新院系表和所有用户的院系名称）")
    @PutMapping("/rename/{oldName}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> rename(@PathVariable String oldName, @RequestBody Map<String, String> body) {
        String newName = body.get("name");
        if (!StringUtils.hasText(newName)) {
            throw new BusinessException(ErrorCode.PARAM_NULL, "院系名称不能为空");
        }
        newName = newName.trim();
        if (newName.length() > 64) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "院系名称不能超过64位");
        }
        if (oldName.equals(newName)) {
            return Result.success();
        }
        Department dept = departmentRepository.findByName(oldName)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "院系不存在"));
        if (departmentRepository.existsByName(newName)) {
            throw new BusinessException(ErrorCode.CONFLICT, "新名称已被使用");
        }
        // 更新院系表
        dept.setName(newName);
        departmentRepository.save(dept);
        // 同步更新用户表
        List<SysUser> users = userRepository.findByDepartment(oldName);
        for (SysUser user : users) {
            user.setDepartment(newName);
        }
        userRepository.saveAll(users);
        return Result.success();
    }

    @Operation(summary = "删除院系（删除院系记录并清空用户院系信息）")
    @DeleteMapping("/{name}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable String name) {
        Department dept = departmentRepository.findByName(name)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "院系不存在"));
        // 清空用户院系
        List<SysUser> users = userRepository.findByDepartment(name);
        for (SysUser user : users) {
            user.setDepartment(null);
        }
        userRepository.saveAll(users);
        // 删除院系记录
        departmentRepository.delete(dept);
        return Result.success();
    }
}