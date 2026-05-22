package com.competition.backend.controller;

import com.competition.backend.common.exception.GlobalExceptionHandler;
import com.competition.backend.common.security.JwtAuthenticationFilter;
import com.competition.backend.service.AiService;
import com.competition.backend.service.impl.KnowledgeBaseServiceImpl;
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

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AiController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("AI 推荐模块 - Controller 层 MockMvc 测试")
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiService aiService;

    @MockBean
    private KnowledgeBaseServiceImpl knowledgeBaseService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ------------------------------------------------------------------ 4.1
    @Nested
    @DisplayName("4.1 智能推荐 POST /api/v1/ai/recommend")
    class Recommend {

        @Test
        @DisplayName("4.1.1 正常推荐请求 - 返回 AI 生成内容")
        void recommend_success() throws Exception {
            String aiResponse = "## 推荐竞赛\n\n根据您的背景，推荐以下竞赛：\n- 全国大学生数学建模竞赛";
            given(aiService.recommend("我是大一学生，擅长 Python 和数学建模，请推荐适合我的竞赛"))
                    .willReturn(aiResponse);

            mockMvc.perform(post("/api/v1/ai/recommend")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("prompt", "我是大一学生，擅长 Python 和数学建模，请推荐适合我的竞赛"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").value(aiResponse));
        }

        @Test
        @DisplayName("4.1.2 prompt 为空字符串 - Controller 透传，返回 AI 内容")
        void recommend_emptyPrompt() throws Exception {
            given(aiService.recommend("")).willReturn("请提供更多信息");

            mockMvc.perform(post("/api/v1/ai/recommend")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"prompt\":\"\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").value("请提供更多信息"));
        }

        @Test
        @DisplayName("4.1.3 请求体缺少 prompt 字段 - map.get 返回 null，透传给 Service")
        void recommend_missingPromptKey() throws Exception {
            given(aiService.recommend(isNull())).willReturn(null);

            mockMvc.perform(post("/api/v1/ai/recommend")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    // ------------------------------------------------------------------ 4.2
    @Nested
    @DisplayName("4.2 手动刷新知识库 POST /api/v1/ai/knowledge/refresh")
    class KnowledgeRefresh {

        @Test
        @DisplayName("4.2.1 正常刷新请求 - 返回启动提示")
        void refresh_success() throws Exception {
            willDoNothing().given(knowledgeBaseService).refreshKnowledgeBase();

            mockMvc.perform(post("/api/v1/ai/knowledge/refresh")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").value("知识库刷新任务已启动"));
        }

        @Test
        @DisplayName("4.2.2 验证 refreshKnowledgeBase() 被调用一次")
        void refresh_serviceInvoked() throws Exception {
            willDoNothing().given(knowledgeBaseService).refreshKnowledgeBase();

            mockMvc.perform(post("/api/v1/ai/knowledge/refresh")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            then(knowledgeBaseService).should().refreshKnowledgeBase();
        }
    }
}
