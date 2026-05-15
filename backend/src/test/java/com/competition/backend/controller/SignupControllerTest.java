package com.competition.backend.controller;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.common.exception.GlobalExceptionHandler;
import com.competition.backend.common.result.PageVO;
import com.competition.backend.common.security.JwtAuthenticationFilter;
import com.competition.backend.entity.IndividualSignup;
import com.competition.backend.service.SignupService;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = SignupController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("报名模块 - Controller 层 MockMvc 测试")
class SignupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SignupService signupService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    @DisplayName("3.1 POST /api/v1/signups/individual")
    class SignUpIndividualTests {
        @Test
        @DisplayName("3.1.1 - 报名成功返回 signupId")
        void signUpIndividual_success() throws Exception {
            given(signupService.signUpIndividual(any()))
                    .willReturn(Map.of("signupId", 88L, "status", "DRAFT"));

            String body = """
                    {
                      "competitionId": 1,
                      "teacherId": 2,
                      "motivation": "希望提升算法能力",
                      "introduction": "掌握Java"
                    }
                    """;

            mockMvc.perform(post("/api/v1/signups/individual")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.signupId").value(88))
                    .andExpect(jsonPath("$.data.status").value("DRAFT"));
        }

        @Test
        @DisplayName("3.1.9 - 缺少必填字段返回 400")
        void signUpIndividual_validationFail() throws Exception {
            String body = """
                    {
                      "teacherId": 2
                    }
                    """;

            mockMvc.perform(post("/api/v1/signups/individual")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000));
        }
    }

    @Nested
    @DisplayName("3.2 POST /api/v1/signups/individual/{id}/submit")
    class SubmitIndividualTests {
        @Test
        @DisplayName("3.2.1 - 提交成功")
        void submitIndividual_success() throws Exception {
            willDoNothing().given(signupService).submitIndividual(eq(11L), any());

            String body = """
                    {
                      "motivation": "更新后的动机",
                      "introduction": "更新后的介绍"
                    }
                    """;

            mockMvc.perform(post("/api/v1/signups/individual/11/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        @Test
        @DisplayName("3.2.3 - 状态不允许提交返回 40132")
        void submitIndividual_statusError() throws Exception {
            willThrow(new BusinessException(ErrorCode.SIGNUP_STATUS_ERROR, "当前状态不允许提交"))
                    .given(signupService).submitIndividual(eq(11L), any());

            mockMvc.perform(post("/api/v1/signups/individual/11/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40132));
        }
    }

    @Nested
    @DisplayName("3.3 GET /api/v1/signups/individual/my")
    class MyIndividualTests {
        @Test
        @DisplayName("3.3.1 - 查询我的报名分页成功")
        void myIndividual_success() throws Exception {
            IndividualSignup signup = IndividualSignup.builder().id(1L).status("DRAFT").build();
            PageVO<?> pageVO = PageVO.<IndividualSignup>builder()
                    .list(List.of(signup))
                    .total(1L)
                    .page(1)
                    .size(10)
                    .totalPages(1)
                    .build();
            doReturn(pageVO).when(signupService).getMyIndividualSignups(1, 10, "DRAFT");

            mockMvc.perform(get("/api/v1/signups/individual/my")
                            .param("page", "1")
                            .param("size", "10")
                            .param("status", "DRAFT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.list[0].status").value("DRAFT"));
        }
    }

    @Nested
    @DisplayName("3.4 POST /api/v1/signups/team")
    class SignUpTeamTests {
        @Test
        @DisplayName("3.4.1 - 团队赛草稿创建成功")
        void signUpTeam_success() throws Exception {
            given(signupService.signUpTeam(101L))
                    .willReturn(Map.of("signupId", 66L, "status", "DRAFT"));

            mockMvc.perform(post("/api/v1/signups/team")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"teamId\":101}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.signupId").value(66));
        }

        @Test
        @DisplayName("3.4.3 - 指导老师未确认返回 40145")
        void signUpTeam_teacherNotConfirmed() throws Exception {
            given(signupService.signUpTeam(101L))
                    .willThrow(new BusinessException(ErrorCode.TEAM_TEACHER_NOT_CONFIRMED, "指导老师尚未确认，不可报名"));

            mockMvc.perform(post("/api/v1/signups/team")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"teamId\":101}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40145));
        }
    }

    @Nested
    @DisplayName("3.5 POST /api/v1/signups/team/{id}/submit")
    class SubmitTeamTests {
        @Test
        @DisplayName("3.5.1 - 团队赛提交成功")
        void submitTeam_success() throws Exception {
            willDoNothing().given(signupService).submitTeam(8L);

            mockMvc.perform(post("/api/v1/signups/team/8/submit"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        @Test
        @DisplayName("3.5.3 - 人数不足返回 40000")
        void submitTeam_memberCountInsufficient() throws Exception {
            willThrow(new BusinessException(ErrorCode.PARAM_ERROR, "队伍人数不足"))
                    .given(signupService).submitTeam(8L);

            mockMvc.perform(post("/api/v1/signups/team/8/submit"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40000));
        }
    }
}
