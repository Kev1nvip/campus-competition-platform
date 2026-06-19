package com.competition.backend.service.impl;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.entity.Competition;
import com.competition.backend.repository.CompetitionRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl {

    private final CompetitionRepository competitionRepository;
    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;

    @Qualifier("aiTaskExecutor")
    private final Executor aiTaskExecutor;

    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    @Value("${ai.vector.table:rag_embeddings}")
    private String vectorTableName;

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
        clearVectorTable();

        // 1) 从 rag_document 同步到 rag_embeddings（含无向量的文本，供关键词检索）
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT content, embedding FROM rag_document WHERE content IS NOT NULL"
        );
        for (Map<String, Object> row : rows) {
            UUID newId = UUID.randomUUID();
            String content = (String) row.get("content");
            Object embeddingObj = row.get("embedding");
            if (embeddingObj != null) {
                String sql = "INSERT INTO " + vectorTableName + " (embedding_id, text, embedding) VALUES (?, ?, ?::vector)";
                jdbcTemplate.update(sql, newId, content, embeddingObj);
            } else {
                jdbcTemplate.update(
                        "INSERT INTO " + vectorTableName + " (embedding_id, text) VALUES (?, ?)",
                        newId, content);
            }
        }
        log.info("Synced {} vectors from rag_document to {}", rows.size(), vectorTableName);

        // 2) 竞赛基本信息（即使无向量也写入文本，供关键词检索）
        List<Competition> competitions = competitionRepository.findAll();
        for (Competition comp : competitions) {
            String text = String.format(
                    "竞赛名称：%s。主办方：%s。参赛要求：%s。详情：%s",
                    safe(comp.getTitle()),
                    safe(comp.getOrganizer()),
                    safe(comp.getRequirement()),
                    safe(comp.getDescription())
            ).trim();
            if (text.isEmpty()) continue;

            UUID newId = UUID.randomUUID();
            jdbcTemplate.update(
                    "INSERT INTO " + vectorTableName + " (embedding_id, text) VALUES (?, ?)",
                    newId, text);
            try {
                Embedding emb = embeddingModel.embed(text).content();
                jdbcTemplate.update(
                        "UPDATE " + vectorTableName + " SET embedding = ?::vector WHERE embedding_id = ?",
                        arrayToPgVector(emb.vector()), newId);
            } catch (Exception e) {
                log.warn("Embedding failed for compId={}, text saved for keyword search. Check SILICONFLOW_API_KEY", comp.getId());
            }
        }

        log.info("Knowledge base refresh done: {} competitions processed", competitions.size());
    }

    private void clearVectorTable() {
        if (!vectorTableName.matches("^[a-zA-Z0-9_]+$")) {
            throw new BusinessException(ErrorCode.AI_KNOWLEDGE_REFRESH_FAIL, "向量表配置非法");
        }
        jdbcTemplate.execute("TRUNCATE TABLE " + vectorTableName);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String arrayToPgVector(float[] vec) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vec[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}