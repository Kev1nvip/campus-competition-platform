package com.competition.backend.controller;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.common.exception.GlobalExceptionHandler;
import com.competition.backend.common.security.JwtAuthenticationFilter;
import com.competition.backend.service.AuthService;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 第五部分：系统与异常验证
 *
 * 复用 AuthController 作为触发入口，通过 Mock AuthService 抛出各类异常，
 * 验证 GlobalExceptionHandler 对每种异常的 HTTP 状态码与响应体格式。
 */
@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("第五部分 - 系统与异常验证（GlobalExceptionHandler）")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String REGISTER_URL = "/api/v1/auth/register";

    private String loginBody(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of("username", username, "password", password));
    }

    // ------------------------------------------------------------------ 5.1
    @Nested
    @DisplayName("5.1 业务异常处理（BusinessException → HTTP 200）")
    class BusinessExceptionHandling {

        @Test
        @DisplayName("5.1.1 密码错误 → HTTP 200, code=40101, message=用户名或密码错误")
        void login_wrongPassword_returns200With40101() throws Exception {
            given(authService.login(any()))
                    .willThrow(new BusinessException(ErrorCode.USER_PASSWORD_ERROR, "用户名或密码错误"));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("student01", "wrongPass1")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40101))
                    .andExpect(jsonPath("$.message").value("用户名或密码错误"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        @DisplayName("5.1.2 账号被禁用 → HTTP 200, code=40102, message=账号已被禁用")
        void login_disabledAccount_returns200With40102() throws Exception {
            given(authService.login(any()))
                    .willThrow(new BusinessException(ErrorCode.USER_DISABLED, "账号已被禁用"));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("disabled_user", "password123")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40102))
                    .andExpect(jsonPath("$.message").value("账号已被禁用"));
        }
    }

    // ------------------------------------------------------------------ 5.2
    @Nested
    @DisplayName("5.2 参数校验异常处理（@Validated → HTTP 400, code=40000）")
    class ValidationExceptionHandling {

        @Test
        @DisplayName("5.2.1 登录请求体为空 JSON → HTTP 400, code=40000，含校验错误信息")
        void login_emptyBody_returns400With40000() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000))
                    .andExpect(jsonPath("$.message").isString());
        }

        @Test
        @DisplayName("5.2.2 注册请求 username 含特殊字符 → HTTP 400, code=40000，含格式错误提示")
        void register_invalidUsername_returns400With40000() throws Exception {
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("username", "test@!");
            payload.put("password", "password123");
            payload.put("realName", "张三");
            payload.put("role", "STUDENT");
            payload.put("studentNo", "20240001");

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000))
                    .andExpect(jsonPath("$.message").value(containsString("用户名只能包含字母、数字和下划线")));
        }
    }

    // ------------------------------------------------------------------ 5.3
    @Nested
    @DisplayName("5.3 认证与权限异常处理")
    class AuthExceptionHandling {

        @Test
        @DisplayName("5.3.1 AuthenticationException → HTTP 401, code=40100, message=未登录或Token已过期")
        void authenticationException_returns401() throws Exception {
            given(authService.login(any()))
                    .willThrow(new AuthenticationException("token expired") {});

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("user01", "password1")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(40100))
                    .andExpect(jsonPath("$.message").value("未登录或Token已过期"));
        }

        @Test
        @DisplayName("5.3.2 AccessDeniedException → HTTP 403, code=40300, message=无操作权限")
        void accessDeniedException_returns403() throws Exception {
            given(authService.login(any()))
                    .willThrow(new AccessDeniedException("forbidden"));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("user01", "password1")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(40300))
                    .andExpect(jsonPath("$.message").value("无操作权限"));
        }
    }

    // ------------------------------------------------------------------ 5.4
    @Nested
    @DisplayName("5.4 兜底异常处理（未预期 RuntimeException → HTTP 500）")
    class FallbackExceptionHandling {

        @Test
        @DisplayName("5.4.1 RuntimeException → HTTP 500, code=50000, message 含异常信息")
        void runtimeException_returns500() throws Exception {
            given(authService.login(any()))
                    .willThrow(new RuntimeException("数据库连接失败"));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("user01", "password1")))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(50000))
                    .andExpect(jsonPath("$.message").value(containsString("数据库连接失败")));
        }
    }
}
