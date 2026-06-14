package com.competition.backend.service;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.config.JwtProperties;
import com.competition.backend.dto.LoginDTO;
import com.competition.backend.dto.RegisterDTO;
import com.competition.backend.entity.SysUser;
import com.competition.backend.repository.SysUserRepository;
import com.competition.backend.service.impl.AuthServiceImpl;
import com.competition.backend.util.JwtUtil;
import com.competition.backend.vo.LoginVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * AuthServiceImpl 单元测试
 * <p>
 * 测试策略：纯单元测试，Mock 所有外部依赖（Repository / PasswordEncoder / JwtUtil）。
 * 不启动 Spring 容器，速度极快。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("认证模块 - Service 层单元测试")
class AuthServiceImplTest {

    @Mock
    private SysUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthServiceImpl authService;

    // ============================================================
    // 辅助方法
    // ============================================================

    private RegisterDTO buildStudentDTO() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("student_test01");
        dto.setPassword("password123");
        dto.setRealName("张学生");
        dto.setRole("STUDENT");
        dto.setStudentNo("20240001");
        dto.setDepartment("计算机学院");
        dto.setEmail("student@school.edu.cn");
        return dto;
    }

    private RegisterDTO buildTeacherDTO() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("teacher_test01");
        dto.setPassword("teacher123");
        dto.setRealName("李老师");
        dto.setRole("TEACHER");
        dto.setTitle("副教授");
        dto.setDepartment("计算机学院");
        return dto;
    }

    private SysUser buildActiveUser(Long id, String username, String encodedPassword, String role) {
        return SysUser.builder()
                .id(id)
                .username(username)
                .password(encodedPassword)
                .realName("测试用户")
                .role(role)
                .status("ACTIVE")
                .build();
    }

    // ============================================================
    // 注册测试（register）
    // ============================================================

    @Nested
    @DisplayName("1.1 用户注册")
    class RegisterTests {

        @Test
        @DisplayName("1.1.1 - 学生正常注册：存库成功，不抛异常")
        void register_student_success() {
            RegisterDTO dto = buildStudentDTO();
            given(userRepository.existsByUsername(dto.getUsername())).willReturn(false);
            given(userRepository.existsByStudentNo(dto.getStudentNo())).willReturn(false);
            given(passwordEncoder.encode(dto.getPassword())).willReturn("hashed_pw");

            authService.register(dto);

            // 验证 save 被调用一次
            then(userRepository).should().save(any(SysUser.class));
        }

        @Test
        @DisplayName("1.1.2 - 教师正常注册：存库成功，不抛异常")
        void register_teacher_success() {
            RegisterDTO dto = buildTeacherDTO();
            given(userRepository.existsByUsername(dto.getUsername())).willReturn(false);
            given(passwordEncoder.encode(dto.getPassword())).willReturn("hashed_pw");

            authService.register(dto);

            then(userRepository).should().save(any(SysUser.class));
        }

        @Test
        @DisplayName("1.1.3 - 用户名已存在：抛 BusinessException，code=40103")
        void register_fail_usernameExists() {
            RegisterDTO dto = buildStudentDTO();
            given(userRepository.existsByUsername(dto.getUsername())).willReturn(true);

            assertThatThrownBy(() -> authService.register(dto))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo(ErrorCode.USER_NAME_EXISTS);
                        assertThat(be.getMessage()).contains("用户名已存在");
                    });

            // save 不应被调用
            then(userRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("1.1.4 - 学号已存在：抛 BusinessException，code=40104")
        void register_fail_studentNoExists() {
            RegisterDTO dto = buildStudentDTO();
            given(userRepository.existsByUsername(dto.getUsername())).willReturn(false);
            given(userRepository.existsByStudentNo(dto.getStudentNo())).willReturn(true);

            assertThatThrownBy(() -> authService.register(dto))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo(ErrorCode.STUDENT_NO_EXISTS);
                        assertThat(be.getMessage()).contains("学号已被注册");
                    });
        }

        @Test
        @DisplayName("1.1.5 - 学生缺少 studentNo：抛 BusinessException，code=40001")
        void register_fail_studentMissingStudentNo() {
            RegisterDTO dto = buildStudentDTO();
            dto.setStudentNo(null); // 学生但无学号
            given(userRepository.existsByUsername(dto.getUsername())).willReturn(false);

            assertThatThrownBy(() -> authService.register(dto))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo(ErrorCode.PARAM_NULL);
                        assertThat(be.getMessage()).contains("学号不能为空");
                    });
        }

        @Test
        @DisplayName("1.1.6 - 教师缺少 title：抛 BusinessException，code=40001")
        void register_fail_teacherMissingTitle() {
            RegisterDTO dto = buildTeacherDTO();
            dto.setTitle(null); // 教师但无职称
            given(userRepository.existsByUsername(dto.getUsername())).willReturn(false);

            assertThatThrownBy(() -> authService.register(dto))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo(ErrorCode.PARAM_NULL);
                        assertThat(be.getMessage()).contains("职称不能为空");
                    });
        }

        @Test
        @DisplayName("1.1.7 - 注册时密码应被 BCrypt 加密后存储")
        void register_passwordShouldBeEncoded() {
            RegisterDTO dto = buildStudentDTO();
            given(userRepository.existsByUsername(anyString())).willReturn(false);
            given(userRepository.existsByStudentNo(anyString())).willReturn(false);
            given(passwordEncoder.encode("password123")).willReturn("$2a$bcrypt$hashed");

            authService.register(dto);

            // 捕获实际存储的实体，验证密码已加密
            org.mockito.ArgumentCaptor<SysUser> captor =
                    org.mockito.ArgumentCaptor.forClass(SysUser.class);
            then(userRepository).should().save(captor.capture());
            SysUser saved = captor.getValue();

            assertThat(saved.getPassword()).isEqualTo("$2a$bcrypt$hashed");
            assertThat(saved.getPassword()).doesNotContain("password123");
            assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        }
    }

    // ============================================================
    // 登录测试（login）
    // ============================================================

    @Nested
    @DisplayName("1.2 用户登录")
    class LoginTests {

        @BeforeEach
        void mockJwtProps() {
            // lenient：jwtProperties.getExpiration() 只在正常登录路径使用，
            // 失败路径不需要，lenient 模式避免 UnnecessaryStubbingException
            org.mockito.Mockito.lenient()
                    .when(jwtProperties.getExpiration())
                    .thenReturn(86400L);
        }

        @Test
        @DisplayName("1.2.1 - 正常登录：返回 token 和 userInfo")
        void login_success() {
            SysUser user = buildActiveUser(1L, "student_test01", "hashed_pw", "STUDENT");
            user.setDepartment("计算机学院");

            LoginDTO dto = new LoginDTO();
            dto.setUsername("student_test01");
            dto.setPassword("password123");

            given(userRepository.findByUsername("student_test01")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("password123", "hashed_pw")).willReturn(true);
            given(jwtUtil.generateToken(1L, "student_test01", "STUDENT")).willReturn("mock.jwt.token");

            LoginVO result = authService.login(dto);

            assertThat(result).isNotNull();
            assertThat(result.getToken()).isEqualTo("mock.jwt.token");
            assertThat(result.getTokenType()).isEqualTo("Bearer");
            assertThat(result.getExpiresIn()).isEqualTo(86400L);
            assertThat(result.getUserInfo()).isNotNull();
            assertThat(result.getUserInfo().getUserId()).isEqualTo(1L);
            assertThat(result.getUserInfo().getUsername()).isEqualTo("student_test01");
            assertThat(result.getUserInfo().getRole()).isEqualTo("STUDENT");
        }

        @Test
        @DisplayName("1.2.2 - 用户名不存在：抛 BusinessException，code=40101")
        void login_fail_userNotFound() {
            LoginDTO dto = new LoginDTO();
            dto.setUsername("no_such_user");
            dto.setPassword("any_password");

            given(userRepository.findByUsername("no_such_user")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(dto))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo(ErrorCode.USER_PASSWORD_ERROR);
                        assertThat(be.getMessage()).contains("用户名或密码错误");
                    });
        }

        @Test
        @DisplayName("1.2.3 - 密码错误：抛 BusinessException，code=40101")
        void login_fail_wrongPassword() {
            SysUser user = buildActiveUser(1L, "student_test01", "hashed_pw", "STUDENT");

            LoginDTO dto = new LoginDTO();
            dto.setUsername("student_test01");
            dto.setPassword("wrong_password");

            given(userRepository.findByUsername("student_test01")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrong_password", "hashed_pw")).willReturn(false);

            assertThatThrownBy(() -> authService.login(dto))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo(ErrorCode.USER_PASSWORD_ERROR);
                        assertThat(be.getMessage()).contains("用户名或密码错误");
                    });
        }

        @Test
        @DisplayName("1.2.4 - 账号被禁用：先检查状态，抛 BusinessException，code=40102")
        void login_fail_userDisabled() {
            SysUser user = buildActiveUser(1L, "disabled_user", "hashed_pw", "STUDENT");
            user.setStatus("DISABLED");

            LoginDTO dto = new LoginDTO();
            dto.setUsername("disabled_user");
            dto.setPassword("password123");

            given(userRepository.findByUsername("disabled_user")).willReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.login(dto))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo(ErrorCode.USER_DISABLED);
                        assertThat(be.getMessage()).contains("账号已被禁用");
                    });

            // 账号禁用时不应进行密码比对
            then(passwordEncoder).should(never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("1.2.7 - 登录成功后 JwtUtil.generateToken 被正确调用（含 userId/username/role）")
        void login_success_tokenGeneratedWithCorrectClaims() {
            SysUser user = buildActiveUser(42L, "teacher_test01", "hashed_pw", "TEACHER");

            LoginDTO dto = new LoginDTO();
            dto.setUsername("teacher_test01");
            dto.setPassword("teacher123");

            given(userRepository.findByUsername("teacher_test01")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("teacher123", "hashed_pw")).willReturn(true);
            given(jwtUtil.generateToken(42L, "teacher_test01", "TEACHER")).willReturn("teacher.jwt.token");

            LoginVO result = authService.login(dto);

            // 验证 generateToken 入参正确
            then(jwtUtil).should().generateToken(42L, "teacher_test01", "TEACHER");
            assertThat(result.getToken()).isEqualTo("teacher.jwt.token");
        }
    }
}
