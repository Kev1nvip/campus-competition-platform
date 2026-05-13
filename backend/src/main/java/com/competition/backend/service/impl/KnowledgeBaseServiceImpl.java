package com.competition.backend.service.impl;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.entity.Competition;
import com.competition.backend.repository.CompetitionRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl {

    private final CompetitionRepository competitionRepository;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    @Qualifier("aiTaskExecutor")
    private final Executor aiTaskExecutor;

    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    public void triggerAsyncRefresh() {
        if (!refreshing.compareAndSet(false, true)) {
            throw new BusinessException(ErrorCode.AI_KNOWLEDGE_REFRESHING, "知识库刷新任务正在执行中，请稍后再试");
        }

        try {
            aiTaskExecutor.execute(() -> {
                long start = System.currentTimeMillis();
                try {
                    refreshKnowledgeBaseInternal();
                    log.info("Knowledge base refresh completed, costMs={}", System.currentTimeMillis() - start);
                } catch (Exception e) {
                    log.error("Knowledge base refresh failed", e);
                } finally {
                    refreshing.set(false);
                }
            });
        } catch (RuntimeException e) {
            refreshing.set(false);
            throw new BusinessException(ErrorCode.AI_KNOWLEDGE_REFRESH_FAIL, "知识库刷新任务启动失败");
        }
    }

    private void refreshKnowledgeBaseInternal() {
        List<Competition> competitions = competitionRepository.findAll();

        // Rebuild from scratch to keep refresh idempotent.
        embeddingStore.removeAll();
        // 简单的分块逻辑：每 500 字一块，重叠 50 字
        DocumentSplitter splitter = DocumentSplitters.recursive(500, 50);
        for (Competition comp : competitions) {
            String text = String.format(
                    "竞赛名称：%s。主办方：%s。参赛要求：%s。详情：%s",
                    safe(comp.getTitle()),
                    safe(comp.getOrganizer()),
                    safe(comp.getRequirement()),
                    safe(comp.getDescription())
            );

            Document doc = Document.from(text);
            
            List<TextSegment> segments = splitter.split(doc);
            if (segments.isEmpty()) {
                continue;
            }

            log.info("Refreshing competition vector, title={}, segments={}", comp.getTitle(), segments.size());
            embeddingStore.addAll(embeddingModel.embedAll(segments).content(), segments);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
