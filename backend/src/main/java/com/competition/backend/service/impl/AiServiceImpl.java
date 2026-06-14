package com.competition.backend.service.impl;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.service.AiAssistant;
import com.competition.backend.service.AiService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ChatLanguageModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final KnowledgeBaseServiceImpl knowledgeBaseService;

    private AiAssistant assistant;

    @PostConstruct
    public void init() {
        // 1. 配置检索器：从 PGVector 中找最相似的 3 条竞赛背景
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                .minScore(0.6) // 相关性阈值
                .build();

        // 2. 配置 RAG 增强器
        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .build();

        // 3. 构建 Assistant
        this.assistant = AiServices.builder(AiAssistant.class)
                .chatLanguageModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10)) // 记住最近 10 轮对话
                .build();
    }

    @Override
    public String recommend(String prompt) {
        String normalizedPrompt = prompt == null ? null : prompt.trim();
        if (normalizedPrompt == null || normalizedPrompt.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "prompt不能为空");
        }

        String promptPreview = normalizedPrompt.length() > 80
                ? normalizedPrompt.substring(0, 80) + "..."
                : normalizedPrompt;
        log.info("AI recommend request received, promptPreview={}", promptPreview);

        try {
            return assistant.chat(normalizedPrompt);
        } catch (RuntimeException e) {
            Throwable root = rootCause(e);
            if (root instanceof TimeoutException) {
                throw new BusinessException(ErrorCode.AI_UPSTREAM_TIMEOUT, "AI服务响应超时，请稍后重试");
            }
            log.error("AI recommend failed", e);
            throw new BusinessException(ErrorCode.AI_RECOMMEND_FAILED, "AI推荐服务暂时不可用，请稍后重试");
        }
    }

    @Override
    public void triggerKnowledgeRefresh() {
        knowledgeBaseService.triggerAsyncRefresh();
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
