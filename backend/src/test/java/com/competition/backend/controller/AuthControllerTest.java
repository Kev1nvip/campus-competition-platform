package com.competition.backend.controller;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.common.exception.GlobalExceptionHandler;
import com.competition.backend.common.security.JwtAuthenticationFilter;
import com.competition.backend.service.AuthService;
import com.competition.backend.vo.LoginVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController MockMvc 测试
 * <p>
 * 测试策略：排除 SecurityAutoConfiguration，禁用 Spring Security 过滤链，
 * 专注验证 HTTP 路由、参数校验（@Validated）、JSON 序列化、异常响应格式。
 * Service 层业务逻辑的完整测试见 AuthServiceImplTest。
 * </p>
 */
@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("认证模块 - Controller 层 MockMvc 测试")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ============================================================
    // 注册接口测试
    // ============================================================

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterControllerTests {

        private Map<String, Object> buildStudentPayload() {
            return Map.of(
                    "username", "student_test01",
                    "password", "password123",
                    "realName", "张学生",
                    "role", "STUDENT",
                    "studentNo", "20240001",
                    "department", "计算机学院",
                    "email", "student@school.edu.cn"
            );
        }

        @Test
        @DisplayName("1.1.1 - 学生正常注册 → 200, code=0")
        void register_student_success_returns200() throws Exception {
            willDoNothing().given(authService).register(any());

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildStudentPayload())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        @DisplayName("1.1.3 - 用户名已存在 → HTTP 200, code=40103")
        void register_fail_usernameExists_returns40103() throws Exception {
            willThrow(new BusinessException(ErrorCode.USER_NAME_EXISTS, "用户名已存在"))
                    .given(authService).register(any());

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildStudentPayload())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40103))
                    .andExpect(jsonPath("$.message").value("用户名已存在"));
        }

        @Test
        @DisplayName("1.1.4 - 学号已存在 → HTTP 200, code=40104")
        void register_fail_studentNoExists_returns40104() throws Exception {
            willThrow(new BusinessException(ErrorCode.STUDENT_NO_EXISTS, "学号已被注册"))
                    .given(authService).register(any());

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildStudentPayload())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40104))
                    .andExpect(jsonPath("$.message").value("学号已被注册"));
        }

        @Test
        @DisplayName("1.1.7 - 密码不含数字 → HTTP 400, code=40000")
        void register_fail_passwordNoDigit_returns400() throws Exception {
            Map<String, Object> payload = new java.util.HashMap<>(buildStudentPayload());
            payload.put("password", "onlyletters"); // 无数字，不满足 @Pattern

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000))
                    .andExpect(jsonPath("$.message").isString());
        }

        @Test
        @DisplayName("1.1.8 - 密码过短（< 8位） → HTTP 400, code=40000")
        void register_fail_passwordTooShort_returns400() throws Exception {
            Map<String, Object> payload = new java.util.HashMap<>(buildStudentPayload());
            payload.put("password", "ab1"); // 3位，低于最小长度

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000));
        }

        @Test
        @DisplayName("1.1.9 - 用户名含特殊字符 → HTTP 400, code=40000")
        void register_fail_usernameSpecialChar_returns400() throws Exception {
            Map<String, Object> payload = new java.util.HashMap<>(buildStudentPayload());
            payload.put("username", "test@!"); // 包含特殊字符

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("用户名只能包含字母、数字和下划线")));
        }

        @Test
        @DisplayName("1.1.10 - role 非法值（GUEST） → HTTP 400, code=40000")
        void register_fail_invalidRole_returns400() throws Exception {
            Map<String, Object> payload = new java.util.HashMap<>(buildStudentPayload());
            payload.put("role", "GUEST");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("角色不合法")));
        }

        @Test
        @DisplayName("1.1.11 - 请求体为空 JSON → HTTP 400，包含多条校验错误")
        void register_fail_emptyBody_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000))
                    .andExpect(jsonPath("$.message").isString()); // 多个错误以分号拼接
        }
    }

    // ============================================================
    // 登录接口测试
    // ============================================================

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginControllerTests {

        private LoginVO buildMockLoginVO() {
            return LoginVO.builder()
                    .token("mock.jwt.token")
                    .tokenType("Bearer")
                    .expiresIn(86400L)
                    .userInfo(LoginVO.UserInfo.builder()
                            .userId(1L)
                            .username("student_test01")
                            .realName("张学生")
                            .role("STUDENT")
                            .department("计算机学院")
                            .avatarUrl(null)
                            .build())
                    .build();
        }

        @Test
        @DisplayName("1.2.1 - 正常登录 → HTTP 200, 含 token 和 userInfo")
        void login_success_returns200WithToken() throws Exception {
            given(authService.login(any())).willReturn(buildMockLoginVO());

            String requestBody = """
                    {
                      "username": "student_test01",
                      "password": "password123"
                    }
                    """;

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.data.token").value("mock.jwt.token"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.expiresIn").value(86400))
                    .andExpect(jsonPath("$.data.userInfo.userId").value(1))
                    .andExpect(jsonPath("$.data.userInfo.username").value("student_test01"))
                    .andExpect(jsonPath("$.data.userInfo.role").value("STUDENT"));
        }

        @Test
        @DisplayName("1.2.2 - 用户名不存在 → HTTP 200, code=40101")
        void login_fail_userNotFound_returns40101() throws Exception {
            given(authService.login(any()))
                    .willThrow(new BusinessException(ErrorCode.USER_PASSWORD_ERROR, "用户名或密码错误"));

            String requestBody = """
                    {
                      "username": "no_such_user",
                      "password": "any_password1"
                    }
                    """;

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40101))
                    .andExpect(jsonPath("$.message").value("用户名或密码错误"));
        }

        @Test
        @DisplayName("1.2.3 - 密码错误 → HTTP 200, code=40101")
        void login_fail_wrongPassword_returns40101() throws Exception {
            given(authService.login(any()))
                    .willThrow(new BusinessException(ErrorCode.USER_PASSWORD_ERROR, "用户名或密码错误"));

            String requestBody = """
                    {
                      "username": "student_test01",
                      "password": "wrong_password1"
                    }
                    """;

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40101))
                    .andExpect(jsonPath("$.message").value("用户名或密码错误"));
        }

        @Test
        @DisplayName("1.2.4 - 账号被禁用 → HTTP 200, code=40102")
        void login_fail_disabled_returns40102() throws Exception {
            given(authService.login(any()))
                    .willThrow(new BusinessException(ErrorCode.USER_DISABLED, "账号已被禁用"));

            String requestBody = """
                    {
                      "username": "disabled_user",
                      "password": "password123"
                    }
                    """;

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40102))
                    .andExpect(jsonPath("$.message").value("账号已被禁用"));
        }

        @Test
        @DisplayName("1.2.5 - username 为空字符串 → HTTP 400, code=40000")
        void login_fail_emptyUsername_returns400() throws Exception {
            String requestBody = """
                    {
                      "username": "",
                      "password": "password123"
                    }
                    """;

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("用户名不能为空")));
        }

        @Test
        @DisplayName("1.2.6 - 请求体缺少 password 字段 → HTTP 400, code=40000")
        void login_fail_missingPassword_returns400() throws Exception {
            String requestBody = """
                    {
                      "username": "student_test01"
                    }
                    """;

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("密码不能为空")));
        }
    }
}
