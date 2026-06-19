package com.competition.backend.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class HybridContentRetriever implements ContentRetriever {

    private final EmbeddingStoreContentRetriever vectorRetriever;
    private final JdbcTemplate jdbcTemplate;

    public HybridContentRetriever(EmbeddingStoreContentRetriever vectorRetriever, JdbcTemplate jdbcTemplate) {
        this.vectorRetriever = vectorRetriever;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Content> retrieve(Query query) {
        try {
            List<Content> vectorResults = vectorRetriever.retrieve(query);
            if (vectorResults != null && !vectorResults.isEmpty()) {
                log.info("Vector retrieval returned {} results", vectorResults.size());
                return vectorResults;
            }
        } catch (Exception e) {
            log.warn("Vector retrieval failed, falling back to keyword search: {}", e.getMessage());
        }

        String keyword = query.text();
        log.info("Keyword fallback search: {}", keyword);
        List<Content> keywordResults = new ArrayList<>();

        // 从 rag_document.content 检索
        List<String> docs = jdbcTemplate.queryForList(
                "SELECT content FROM rag_document WHERE content ILIKE ? LIMIT 5",
                String.class, "%" + keyword + "%");
        for (String doc : docs) {
            keywordResults.add(Content.from(TextSegment.from(doc)));
        }

        // 从 competition 表检索
        jdbcTemplate.query(
                "SELECT title, description, requirement FROM competition WHERE title ILIKE ? OR description ILIKE ? OR requirement ILIKE ? LIMIT 3",
                rs -> {
                    String title = rs.getString("title");
                    String desc = rs.getString("description");
                    String req = rs.getString("requirement");
                    StringBuilder sb = new StringBuilder("竞赛名称：" + (title != null ? title : ""));
                    if (desc != null) sb.append("。详情：" + desc);
                    if (req != null) sb.append("。参赛要求：" + req);
                    keywordResults.add(Content.from(TextSegment.from(sb.toString())));
                },
                "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%");

        log.info("Keyword fallback returned {} results", keywordResults.size());
        return keywordResults;
    }
}