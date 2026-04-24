package com.competition.backend.service.impl;

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
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl {

    private final CompetitionRepository competitionRepository;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    /**
     * 将所有竞赛的描述信息存入向量数据库
     */
    public void refreshKnowledgeBase() {
        List<Competition> competitions = competitionRepository.findAll();
        
        for (Competition comp : competitions) {
            String text = String.format("竞赛名称：%s。主办方：%s。参赛要求：%s。详情：%s",
                    comp.getTitle(), comp.getOrganizer(), comp.getRequirement(), comp.getDescription());
            
            Document doc = Document.from(text);
            // 简单的分块逻辑：每 500 字一块，重叠 50 字
            DocumentSplitter splitter = DocumentSplitters.recursive(500, 50);
            List<TextSegment> segments = splitter.split(doc);

            log.info("正在为竞赛 [{}] 生成向量并存入知识库...", comp.getTitle());
            embeddingStore.addAll(embeddingModel.embedAll(segments).content(), segments);
        }
        log.info("知识库更新完成！");
    }
}