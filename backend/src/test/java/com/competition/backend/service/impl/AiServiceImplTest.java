package com.competition.backend.service.impl;

import com.competition.backend.common.exception.BusinessException;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {

    @Mock
    private ChatLanguageModel chatModel;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private EmbeddingStore<?> embeddingStore;

    @Mock
    private KnowledgeBaseServiceImpl knowledgeBaseService;

    private AiServiceImpl aiService;

    @BeforeEach
    void setUp() {
        @SuppressWarnings("unchecked")
        EmbeddingStore<dev.langchain4j.data.segment.TextSegment> castStore =
                (EmbeddingStore<dev.langchain4j.data.segment.TextSegment>) embeddingStore;
        aiService = new AiServiceImpl(chatModel, embeddingModel, castStore, knowledgeBaseService);
    }

    @Test
    void recommend_shouldThrowParamError_whenPromptBlank() {
        BusinessException e = assertThrows(BusinessException.class, () -> aiService.recommend("  "));
        assertEquals(40000, e.getCode());
    }

    @Test
    void recommend_shouldMapTimeoutError_whenUpstreamTimeout() throws Exception {
        Object assistantProxy = java.lang.reflect.Proxy.newProxyInstance(
                AiServiceImpl.class.getClassLoader(),
                new Class[]{com.competition.backend.service.AiAssistant.class},
                (proxy, method, args) -> {
                    throw new RuntimeException(new TimeoutException("timeout"));
                }
        );

        Field assistantField = AiServiceImpl.class.getDeclaredField("assistant");
        assistantField.setAccessible(true);
        assistantField.set(aiService, assistantProxy);

        BusinessException e = assertThrows(BusinessException.class, () -> aiService.recommend("hello"));
        assertEquals(40201, e.getCode());
    }

    @Test
    void triggerKnowledgeRefresh_shouldDelegateToKnowledgeService() {
        aiService.triggerKnowledgeRefresh();
        Mockito.verify(knowledgeBaseService).triggerAsyncRefresh();
    }
}
