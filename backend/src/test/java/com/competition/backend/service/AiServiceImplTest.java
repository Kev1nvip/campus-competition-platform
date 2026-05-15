package com.competition.backend.service;

import com.competition.backend.service.impl.AiServiceImpl;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("AI 推荐模块 - Service 层单元测试")
class AiServiceImplTest {

    @Mock
    private ChatLanguageModel chatModel;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @Mock
    private AiAssistant assistant;

    private AiServiceImpl aiService;

    @BeforeEach
    void setUp() {
        aiService = new AiServiceImpl(chatModel, embeddingModel, embeddingStore);
        // 跳过 @PostConstruct 中的 LangChain4j 初始化，直接注入 Mock assistant
        ReflectionTestUtils.setField(aiService, "assistant", assistant);
    }

    // ------------------------------------------------------------------ 4.3
    @Nested
    @DisplayName("4.3 AiServiceImpl 单元测试")
    class RecommendService {

        @Test
        @DisplayName("4.3.1 recommend 正常返回 - 透传 assistant.chat() 结果")
        void recommend_returnsAssistantResponse() {
            String prompt = "我是大一学生，擅长 Python 和数学建模，请推荐适合我的竞赛";
            String expected = "## 推荐竞赛\n\n- 全国大学生数学建模竞赛";
            given(assistant.chat(prompt)).willReturn(expected);

            String result = aiService.recommend(prompt);

            assertThat(result).isEqualTo(expected);
            then(assistant).should().chat(prompt);
        }

        @Test
        @DisplayName("4.3.2 prompt 为 null - 透传给 assistant，返回空字符串")
        void recommend_nullPrompt_returnsEmpty() {
            given(assistant.chat(null)).willReturn("");

            String result = aiService.recommend(null);

            assertThat(result).isEmpty();
            then(assistant).should().chat(null);
        }
    }
}
