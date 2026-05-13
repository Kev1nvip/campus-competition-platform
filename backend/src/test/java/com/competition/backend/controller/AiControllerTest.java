package com.competition.backend.controller;

import com.competition.backend.common.security.JwtAuthenticationFilter;
import com.competition.backend.config.SecurityConfig;
import com.competition.backend.dto.AiRecommendRequest;
import com.competition.backend.service.AiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AiController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiService aiService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void recommend_shouldReturnSuccess_whenPromptValid() throws Exception {
        AiRecommendRequest request = new AiRecommendRequest();
        request.setPrompt("Recommend competitions for a freshman");

        Mockito.when(aiService.recommend("Recommend competitions for a freshman"))
                .thenReturn("- Competition A\n- Competition B");

        mockMvc.perform(post("/api/v1/ai/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("- Competition A\n- Competition B"));
    }

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void recommend_shouldReturnBadRequest_whenPromptBlank() throws Exception {
        AiRecommendRequest request = new AiRecommendRequest();
        request.setPrompt("  ");

        mockMvc.perform(post("/api/v1/ai/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void refresh_shouldReturnForbidden_whenNonAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/ai/knowledge/refresh"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void refresh_shouldReturnSuccess_whenAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/ai/knowledge/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Mockito.verify(aiService).triggerKnowledgeRefresh();
    }
}
