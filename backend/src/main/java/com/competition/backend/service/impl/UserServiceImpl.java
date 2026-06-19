package com.competition.backend.service.impl;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.common.result.PageVO;
import com.competition.backend.dto.UserUpdateDTO;
import com.competition.backend.entity.SysUser;
import com.competition.backend.repository.SysUserRepository;
import com.competition.backend.service.UserService;
import com.competition.backend.util.SecurityUtil;
import com.competition.backend.vo.TeacherProfileVO;
import com.competition.backend.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    @Override
    public PageVO<UserInfoVO> listUsers(int page, int size, String keyword) {
        Specification<SysUser> spec = (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) return cb.conjunction();
            String like = "%" + keyword + "%";
            return cb.or(
                    cb.like(root.get("username"), like),
                    cb.like(root.get("realName"), like)
            );
        };
        Page<SysUser> pageResult = userRepository.findAll(
                spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageVO.of(pageResult, this::toVO);
    }

    private UserInfoVO toVO(SysUser user) {
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

    @Override
    public PageVO<TeacherProfileVO> listTeachers(int page, int size, String keyword) {
        Specification<SysUser> spec = (root, query, cb) -> {
            // 只查 TEACHER 角色且账号正常
            jakarta.persistence.criteria.Predicate roleP = cb.equal(root.get("role"), "TEACHER");
            jakarta.persistence.criteria.Predicate statusP = cb.equal(root.get("status"), "ACTIVE");
            if (!StringUtils.hasText(keyword)) return cb.and(roleP, statusP);
            String like = "%" + keyword + "%";
            jakarta.persistence.criteria.Predicate kwP = cb.or(
                    cb.like(root.get("realName"), like),
                    cb.like(root.get("department"), like)
            );
            return cb.and(roleP, statusP, kwP);
        };
        Page<SysUser> pageResult = userRepository.findAll(
                spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "realName")));
        return PageVO.of(pageResult, user -> {
            TeacherProfileVO vo = new TeacherProfileVO();
            vo.setId(user.getId());
            vo.setRealName(user.getRealName());
            vo.setDepartment(user.getDepartment());
            vo.setTitle(user.getTitle());
            vo.setEmail(user.getEmail());
            vo.setAvatarUrl(user.getAvatarUrl());
            return vo;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleUserStatus(Long userId, String status) {
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "状态只能是 ACTIVE 或 DISABLED");
        }
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId.equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能禁用或启用自己");
        }
        
        user.setStatus(status);
        userRepository.save(user);
    }
}