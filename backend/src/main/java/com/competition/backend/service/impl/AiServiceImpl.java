package com.competition.backend.service.impl;

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

@Slf4j
//@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ChatLanguageModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    
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
        log.info("AI 推荐请求: {}", prompt);
        return assistant.chat(prompt);
    }
}