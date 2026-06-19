package com.competition.backend.service;

import com.competition.backend.entity.CompetitionDocument;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfChunkService {

    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;

    @Value("${ai.vector.table:rag_document}")
    private String vectorTableName;

    private static final List<Pattern> CHAPTER_PATTERNS = List.of(
            Pattern.compile("^第[一二三四五六七八九十百千万]+[章节篇部]"),
            Pattern.compile("(?i)^chapter\\s+\\d+"),
            Pattern.compile("(?i)^section\\s+\\d+"),
            Pattern.compile("^\\d+\\.\\d+\\s+\\S"),
            Pattern.compile("^\\d+[、．\\.]\\s*\\S"),
            Pattern.compile("^[一二三四五六七八九十]+[、]\\s*\\S"),
            Pattern.compile("^\\([一二三四五六七八九十]+\\)"),
            Pattern.compile("^\\(\\d+\\)"),
            Pattern.compile("^#{2,3}\\s+\\S")
    );

    public void parseAndSave(InputStream pdfStream, CompetitionDocument doc) {
        try (PDDocument pdf = Loader.loadPDF(pdfStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(pdf);
            if (fullText.isBlank()) {
                log.warn("PDF 内容为空, docId={}", doc.getId());
                return;
            }

            List<String> chunks = splitByChapters(fullText);
            log.info("PDF parsed, docId={}, fileName={}, chunks={}", doc.getId(), doc.getFileName(), chunks.size());

            String compName = resolveCompetitionName(doc.getCompetitionId());

            // 1) 先插入文本行，获取自增 id
            List<Long> insertedIds = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                String content = chunks.get(i).trim();
                if (content.isEmpty()) continue;

                String sql = "INSERT INTO rag_document"
                        + " (doc_name, competition_name, chunk_index, content, document_id, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, NOW())";
                jdbcTemplate.update(sql, doc.getFileName(), compName, i, content, doc.getId());

                // 取最后插入的 id
                Long lastId = jdbcTemplate.queryForObject(
                        "SELECT LASTVAL()", Long.class);
                insertedIds.add(lastId);
            }

            // 2) 批量向量化并回填 embedding
            if (!insertedIds.isEmpty()) {
                List<String> textList = chunks.stream()
                        .filter(c -> !c.trim().isEmpty())
                        .map(String::trim).toList();

                try {
                    List<TextSegment> segments = textList.stream()
                            .map(TextSegment::from)
                            .collect(Collectors.toList());
                    List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
                    for (int i = 0; i < insertedIds.size() && i < embeddings.size(); i++) {
                        Long rowId = insertedIds.get(i);
                        float[] vec = embeddings.get(i).vector();
                        String vectorLiteral = arrayToPgVector(vec);
                        jdbcTemplate.update(
                                "UPDATE rag_document SET embedding = ?::vector WHERE id = ?",
                                vectorLiteral, rowId);

                        String chunkText = textList.get(i);
                        UUID embId = UUID.randomUUID();
                        jdbcTemplate.update(
                                "INSERT INTO rag_embeddings (embedding_id, text, embedding) VALUES (?, ?, ?::vector)",
                                embId, chunkText, vectorLiteral);
                    }
                    log.info("Vectorized {} chunks for docId={}", insertedIds.size(), doc.getId());
                } catch (Exception e) {
                    log.warn("Embedding failed for docId={}, saving text-only to rag_embeddings for keyword search", doc.getId());
                    // 向量化失败时，仍写入文本到 rag_embeddings（供关键词检索）
                    for (int i = 0; i < insertedIds.size(); i++) {
                        String chunkText = textList.size() > i ? textList.get(i) : "";
                        if (chunkText.isEmpty()) continue;
                        UUID embId = UUID.randomUUID();
                        jdbcTemplate.update(
                                "INSERT INTO rag_embeddings (embedding_id, text) VALUES (?, ?)",
                                embId, chunkText);
                    }
                }
            }

        } catch (Exception e) {
            log.error("PDF parse/vectorize failed, docId={}", doc.getId(), e);
            throw new RuntimeException("PDF解析或向量化失败: " + e.getMessage(), e);
        }
    }

    List<String> splitByChapters(String text) {
        List<String> chunks = new ArrayList<>();
        String[] lines = text.split("\n");
        StringBuilder currentChapter = new StringBuilder();
        boolean inChapter = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            if (isChapterStart(trimmed)) {
                if (inChapter && currentChapter.length() > 0) {
                    splitLongChapter(currentChapter.toString().trim(), chunks);
                    currentChapter = new StringBuilder();
                }
                inChapter = true;
                currentChapter.append(trimmed).append("\n");
            } else if (inChapter) {
                currentChapter.append(trimmed).append("\n");
            } else {
                currentChapter.append(trimmed).append("\n");
            }
        }

        if (currentChapter.length() > 0) {
            splitLongChapter(currentChapter.toString().trim(), chunks);
        }

        if (chunks.isEmpty() && !text.isBlank()) {
            chunks.add(text.trim());
        }

        return chunks;
    }

    private boolean isChapterStart(String line) {
        for (Pattern p : CHAPTER_PATTERNS) {
            Matcher m = p.matcher(line);
            if (m.find()) return true;
        }
        return false;
    }

    private void splitLongChapter(String chapter, List<String> chunks) {
        if (chapter.length() <= 2000) {
            chunks.add(chapter);
            return;
        }
        String[] paragraphs = chapter.split("\n\n+");
        StringBuilder part = new StringBuilder();
        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) continue;
            if (part.length() + trimmed.length() > 2000 && part.length() > 0) {
                chunks.add(part.toString().trim());
                part = new StringBuilder();
            }
            part.append(trimmed).append("\n\n");
        }
        if (part.length() > 0) {
            chunks.add(part.toString().trim());
        }
    }

    private String resolveCompetitionName(Long competitionId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT title FROM competition WHERE id = ?", String.class, competitionId);
        } catch (Exception e) {
            return "竞赛#" + competitionId;
        }
    }

    public void deleteByDocumentId(Long documentId) {
        List<String> contents = jdbcTemplate.queryForList(
                "SELECT content FROM rag_document WHERE document_id = ?",
                String.class, documentId);
        for (String content : contents) {
            jdbcTemplate.update("DELETE FROM rag_embeddings WHERE text = ?", content);
        }
        String sql = "DELETE FROM rag_document WHERE document_id = ?";
        jdbcTemplate.update(sql, documentId);
        log.info("Deleted rag_document and rag_embeddings records, documentId={}", documentId);
    }

    /** float[] → PG vector 字面量 '[0.1,0.2,0.3]' */
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