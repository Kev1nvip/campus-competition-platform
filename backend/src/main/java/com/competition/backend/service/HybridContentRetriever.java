package com.competition.backend.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;
import java.util.stream.Collectors;

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
        String keyword = query.text();

        // 1) 向量检索
        try {
            List<Content> vectorResults = vectorRetriever.retrieve(query);
            if (vectorResults != null && !vectorResults.isEmpty()) {
                log.info("Vector retrieval returned {} results", vectorResults.size());
                return vectorResults;
            }
        } catch (Exception e) {
            log.warn("Vector retrieval failed, falling back to keyword search: {}", e.getMessage());
        }

        log.info("Keyword fallback search: {}", keyword);
        List<Content> keywordResults = new ArrayList<>();

        // 2) 关键词拆词（支持中文分词效果）
        Set<String> tokens = tokenize(keyword);

        // 3) rag_document 关键词匹配 + 上下文扩展
        Set<Long> seenDocIds = new HashSet<>();
        for (String token : tokens) {
            if (token.length() < 2) continue;
            List<Map<String, Object>> matchedChunks = jdbcTemplate.queryForList(
                    "SELECT document_id, chunk_index, content FROM rag_document WHERE content ILIKE ? LIMIT 10",
                    "%" + token + "%");
            for (Map<String, Object> row : matchedChunks) {
                Number docIdNum = (Number) row.get("document_id");
                if (docIdNum == null) continue;
                Long docId = docIdNum.longValue();
                if (seenDocIds.contains(docId)) continue;
                seenDocIds.add(docId);

                int matchedIndex = ((Number) row.get("chunk_index")).intValue();
                // 取匹配块前后各 3 块（共 7 块）作为完整上下文
                int start = Math.max(0, matchedIndex - 3);
                int end = matchedIndex + 3;
                List<Map<String, Object>> surrounding = jdbcTemplate.queryForList(
                        "SELECT chunk_index, content FROM rag_document WHERE document_id = ? AND chunk_index BETWEEN ? AND ? ORDER BY chunk_index",
                        docId, start, end);
                StringBuilder combined = new StringBuilder();
                for (Map<String, Object> s : surrounding) {
                    combined.append((String) s.get("content")).append("\n");
                }
                if (combined.length() > 0) {
                    keywordResults.add(Content.from(TextSegment.from(combined.toString().trim())));
                }
            }
        }

        // 4) competition 表关键词匹配
        for (String token : tokens) {
            if (token.length() < 2) continue;
            jdbcTemplate.query(
                    "SELECT title, description, requirement FROM competition WHERE title ILIKE ? OR description ILIKE ? OR requirement ILIKE ? LIMIT 5",
                    rs -> {
                        String title = rs.getString("title");
                        String desc = rs.getString("description");
                        String req = rs.getString("requirement");
                        StringBuilder sb = new StringBuilder("竞赛名称：" + (title != null ? title : ""));
                        if (desc != null) sb.append("。详情：" + desc);
                        if (req != null) sb.append("。参赛要求：" + req);
                        keywordResults.add(Content.from(TextSegment.from(sb.toString())));
                    },
                    "%" + token + "%", "%" + token + "%", "%" + token + "%");
        }

        // 去重（按文本内容去重）
        List<Content> deduped = keywordResults.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(c -> c.textSegment().text(), c -> c, (a, b) -> a, LinkedHashMap::new),
                        m -> new ArrayList<>(m.values())));

        log.info("Keyword fallback returned {} results (deduped from {})", deduped.size(), keywordResults.size());
        return deduped;
    }

    /**
     * 简单拆词：按中文/英文/数字边界切分，每个 2-10 字的片段
     */
    private Set<String> tokenize(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        // 原文整体作为关键词
        if (text.length() <= 40) tokens.add(text);

        // 按常见分隔符拆
        String[] parts = text.split("[\\s,，。；;、！!？?：:（）()\\[\\]【】]+");
        for (String part : parts) {
            if (part.length() < 2) continue;
            if (part.length() <= 10) tokens.add(part);
            // 长词滑动窗口取 4-6 字子串
            if (part.length() > 4) {
                for (int i = 0; i <= part.length() - 4; i++) {
                    tokens.add(part.substring(i, Math.min(i + 6, part.length())));
                }
            }
        }
        return tokens;
    }
}