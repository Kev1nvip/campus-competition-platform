package com.competition.backend.service.impl;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.dto.UserUpdateDTO;
import com.competition.backend.entity.SysUser;
import com.competition.backend.repository.SysUserRepository;
import com.competition.backend.service.UserService;
import com.competition.backend.vo.TeacherProfileVO;
import com.competition.backend.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserRepository userRepository;

    // =============================================
    // 接口1：获取当前登录用户的个人信息
    // =============================================
    @Override
    public UserInfoVO getUserInfo(Long userId) {
        // 1. 根据ID查数据库
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.USER_NOT_FOUND, "用户不存在"));

        // 2. 把 Entity 转换成 VO 返回（手动赋值，只暴露需要的字段）
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setStudentNo(user.getStudentNo());
        vo.setDepartment(user.getDepartment());
        vo.setTitle(user.getTitle());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());

        return vo;
    }

    // =============================================
    // 接口2：更新当前登录用户的个人信息
    // =============================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(Long userId, UserUpdateDTO dto) {
        // 1. 查出当前用户
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.USER_NOT_FOUND, "用户不存在"));

        // 2. 逐个判断字段是否有值，有值才更新（防止把已有数据清空）
        if (StringUtils.hasText(dto.getRealName())) {
            user.setRealName(dto.getRealName());
        }
        if (StringUtils.hasText(dto.getPhone())) {
            user.setPhone(dto.getPhone());
        }
        if (StringUtils.hasText(dto.getEmail())) {
            user.setEmail(dto.getEmail());
        }
        if (StringUtils.hasText(dto.getDepartment())) {
            user.setDepartment(dto.getDepartment());
        }
        if (StringUtils.hasText(dto.getAvatarUrl())) {
            user.setAvatarUrl(dto.getAvatarUrl());
        }
        // 职称只有教师才能改
        if (StringUtils.hasText(dto.getTitle())) {
            if (!"TEACHER".equals(user.getRole())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "只有教师才能设置职称");
            }
            user.setTitle(dto.getTitle());
        }

        // 3. 保存（JPA会自动更新 updated_at 字段）
        userRepository.save(user);
    }

    // =============================================
    // 接口3：获取教师主页公开信息
    // =============================================
    @Override
    public TeacherProfileVO getTeacherProfile(Long teacherId) {
        // 1. 查询用户
        SysUser user = userRepository.findById(teacherId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.USER_NOT_FOUND, "用户不存在"));

        // 2. 必须是教师角色
        if (!"TEACHER".equals(user.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "该用户不是教师");
        }

        // 3. 只返回公开字段（不返回手机号等隐私信息）
        TeacherProfileVO vo = new TeacherProfileVO();
        vo.setId(user.getId());
        vo.setRealName(user.getRealName());
        vo.setDepartment(user.getDepartment());
        vo.setTitle(user.getTitle());
        vo.setEmail(user.getEmail());
        vo.setAvatarUrl(user.getAvatarUrl());

        return vo;
    }
}