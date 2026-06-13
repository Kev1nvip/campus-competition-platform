package com.competition.backend.service.impl;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.config.JwtProperties;
import com.competition.backend.dto.LoginDTO;
import com.competition.backend.dto.RegisterDTO;
import com.competition.backend.entity.SysUser;
import com.competition.backend.repository.SysUserRepository;
import com.competition.backend.service.AuthService;
import com.competition.backend.util.JwtUtil;
import com.competition.backend.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO registerDTO) {
        // 1. 校验用户名是否存在
        if (userRepository.existsByUsername(registerDTO.getUsername())) {
            throw new BusinessException(ErrorCode.USER_NAME_EXISTS, "用户名已存在");
        }

        // 2. 校验角色逻辑
        if ("STUDENT".equals(registerDTO.getRole())) {
            if (!StringUtils.hasText(registerDTO.getStudentNo())) {
                throw new BusinessException(ErrorCode.PARAM_NULL, "学号不能为空");
            }
            if (userRepository.existsByStudentNo(registerDTO.getStudentNo())) {
                throw new BusinessException(ErrorCode.STUDENT_NO_EXISTS, "学号已被注册");
            }
        } else if ("TEACHER".equals(registerDTO.getRole())) {
            if (!StringUtils.hasText(registerDTO.getTitle())) {
                throw new BusinessException(ErrorCode.PARAM_NULL, "职称不能为空");
            }
        }

        // 3. 构建用户实体并保存
        // TEACHER 角色不存学号（null），避免空字符串触发唯一约束
        String studentNo = "STUDENT".equals(registerDTO.getRole())
                ? registerDTO.getStudentNo()
                : null;

        SysUser user = SysUser.builder()
                .username(registerDTO.getUsername())
                .password(passwordEncoder.encode(registerDTO.getPassword()))
                .realName(registerDTO.getRealName())
                .role(registerDTO.getRole())
                .phone(StringUtils.hasText(registerDTO.getPhone()) ? registerDTO.getPhone() : null)
                .email(StringUtils.hasText(registerDTO.getEmail()) ? registerDTO.getEmail() : null)
                .studentNo(studentNo)
                .department(StringUtils.hasText(registerDTO.getDepartment()) ? registerDTO.getDepartment() : null)
                .title(registerDTO.getTitle())
                .status("ACTIVE")
                .build();

        userRepository.save(user);
    }

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        // 1. 查询用户
        SysUser user = userRepository.findByUsername(loginDTO.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_PASSWORD_ERROR, "用户名或密码错误"));

        // 2. 校验状态
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED, "账号已被禁用");
        }

        // 3. 校验密码
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR, "用户名或密码错误");
        }

        // 4. 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 5. 构建返回数据
        return LoginVO.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpiration())
                .userInfo(LoginVO.UserInfo.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .realName(user.getRealName())
                        .role(user.getRole())
                        .department(user.getDepartment())
                        .avatarUrl(user.getAvatarUrl())
                        .build())
                .build();
    }
}