package com.competition.backend.controller;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.common.exception.GlobalExceptionHandler;
import com.competition.backend.common.result.PageVO;
import com.competition.backend.common.security.JwtAuthenticationFilter;
import com.competition.backend.entity.Competition;
import com.competition.backend.service.CompetitionService;
import com.competition.backend.vo.CompetitionListVO;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CompetitionController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("竞赛模块 - Controller 层 MockMvc 测试")
class CompetitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompetitionService competitionService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private Map<String, Object> buildCreatePayload() {
        return Map.of(
                "title", "蓝桥杯软件大赛",
                "type", "INDIVIDUAL",
                "organizer", "工信部",
                "signupStart", "2026-05-01T00:00:00+08:00",
                "signupEnd", "2026-06-01T00:00:00+08:00",
                "competitionStart", "2026-06-15T00:00:00+08:00",
                "competitionEnd", "2026-06-30T00:00:00+08:00",
                "hasQuota", true,
                "maxQuota", 100,
                "description", "算法竞赛"
        );
    }

    @Nested
    @DisplayName("2.1 GET /api/v1/competitions")
    class ListTests {
        @Test
        @DisplayName("2.1.1 - 分页列表查询成功")
        void list_success() throws Exception {
            CompetitionListVO vo = new CompetitionListVO();
            // vo.setCompetitionId(1L);
            vo.setTitle("竞赛A");
            vo.setType("INDIVIDUAL");
            vo.setStatus("SIGNING");

            PageVO<CompetitionListVO> pageVO = PageVO.<CompetitionListVO>builder()
                    .list(List.of(vo))
                    .total(1L)
                    .page(1)
                    .size(10)
                    .totalPages(1)
                    .build();

            given(competitionService.getCompetitionList(1, 10, "SIGNING", "INDIVIDUAL", null)).willReturn(pageVO);

            mockMvc.perform(get("/api/v1/competitions")
                            .param("page", "1")
                            .param("size", "10")
                            .param("status", "SIGNING")
                            .param("type", "INDIVIDUAL"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.list[0].competitionId").value(1));
        }
    }

    @Nested
    @DisplayName("2.2 GET /api/v1/competitions/{id}")
    class DetailTests {
        @Test
        @DisplayName("2.2.1 - 查询竞赛详情成功")
        void detail_success() throws Exception {
            Competition c = Competition.builder()
                    .id(1L)
                    .title("竞赛A")
                    .type("INDIVIDUAL")
                    .status("SIGNING")
                    .createdBy(1L)
                    .signupStart(OffsetDateTime.parse("2026-05-01T00:00:00+08:00"))
                    .signupEnd(OffsetDateTime.parse("2026-06-01T00:00:00+08:00"))
                    .hasQuota(true)
                    .maxQuota(100)
                    .enrolledCount(10)
                    .build();
            given(competitionService.getCompetitionDetail(1L)).willReturn(c);

            mockMvc.perform(get("/api/v1/competitions/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.title").value("竞赛A"));
        }

        @Test
        @DisplayName("2.2.2 - 竞赛不存在返回 40400")
        void detail_notFound() throws Exception {
            given(competitionService.getCompetitionDetail(999L))
                    .willThrow(new BusinessException(ErrorCode.NOT_FOUND, "竞赛不存在"));

            mockMvc.perform(get("/api/v1/competitions/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40400))
                    .andExpect(jsonPath("$.message").value("竞赛不存在"));
        }
    }

    @Nested
    @DisplayName("2.3 POST /api/v1/competitions")
    class CreateTests {
        @Test
        @DisplayName("2.3.1 - 正常发布返回竞赛ID")
        void create_success() throws Exception {
            given(competitionService.createCompetition(any())).willReturn(101L);

            mockMvc.perform(post("/api/v1/competitions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreatePayload())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").value(101));
        }

        @Test
        @DisplayName("2.3.2 - 重复发布返回 40124")
        void create_duplicate() throws Exception {
            given(competitionService.createCompetition(any()))
                    .willThrow(new BusinessException(ErrorCode.COMPETITION_EXISTS, "该竞赛已发布，请勿重复操作"));

            mockMvc.perform(post("/api/v1/competitions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreatePayload())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40124));
        }

        @Test
        @DisplayName("2.3.6 - 必填字段缺失返回 400")
        void create_validationFail() throws Exception {
            String body = """
                    {
                      "type":"INDIVIDUAL",
                      "organizer":"工信部",
                      "signupStart":"2026-05-01T00:00:00+08:00",
                      "signupEnd":"2026-06-01T00:00:00+08:00",
                      "hasQuota":true
                    }
                    """;

            mockMvc.perform(post("/api/v1/competitions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000));
        }
    }

    @Nested
    @DisplayName("2.4 PUT /api/v1/competitions/{id}")
    class UpdateTests {
        @Test
        @DisplayName("2.4.1 - 修改成功")
        void update_success() throws Exception {
            willDoNothing().given(competitionService).updateCompetition(eq(1L), any());

            mockMvc.perform(put("/api/v1/competitions/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreatePayload())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    @Nested
    @DisplayName("2.5 PATCH /api/v1/competitions/{id}/status")
    class StatusTests {
        @Test
        @DisplayName("2.5.1 - OFFLINE 状态变更成功")
        void status_offline_success() throws Exception {
            willDoNothing().given(competitionService).changeStatus(1L, "OFFLINE");

            mockMvc.perform(patch("/api/v1/competitions/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"action\":\"OFFLINE\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }
}
