package com.competition.backend.service.impl;

import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.entity.Competition;
import com.competition.backend.repository.CompetitionRepository;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceImplTest {

    @Mock
    private CompetitionRepository competitionRepository;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private KnowledgeBaseServiceImpl serviceWithNoopExecutor;

    @BeforeEach
    void setUp() {
        Executor holdExecutor = command -> { };
        serviceWithNoopExecutor = new KnowledgeBaseServiceImpl(
                competitionRepository,
                embeddingModel,
                embeddingStore,
                jdbcTemplate,
                holdExecutor
        );
        ReflectionTestUtils.setField(serviceWithNoopExecutor, "vectorTableName", "rag_document");
    }

    @Test
    void triggerAsyncRefresh_shouldRejectWhenAlreadyRefreshing() {
        serviceWithNoopExecutor.triggerAsyncRefresh();
        BusinessException e = assertThrows(BusinessException.class, serviceWithNoopExecutor::triggerAsyncRefresh);
        assertEquals(40202, e.getCode());
    }

    @Test
    void triggerAsyncRefresh_shouldMapStartFailure() {
        Executor brokenExecutor = command -> {
            throw new RuntimeException("executor unavailable");
        };
        KnowledgeBaseServiceImpl service = new KnowledgeBaseServiceImpl(
                competitionRepository,
                embeddingModel,
                embeddingStore,
                jdbcTemplate,
                brokenExecutor
        );
        ReflectionTestUtils.setField(service, "vectorTableName", "rag_document");

        BusinessException e = assertThrows(BusinessException.class, service::triggerAsyncRefresh);
        assertEquals(40203, e.getCode());
    }

    @Test
    void triggerAsyncRefresh_shouldRebuildEmbeddingStore() {
        Mockito.when(competitionRepository.findAll()).thenReturn(Collections.emptyList());

        Executor directExecutor = Runnable::run;
        KnowledgeBaseServiceImpl service = new KnowledgeBaseServiceImpl(
                competitionRepository,
                embeddingModel,
                embeddingStore,
                jdbcTemplate,
                directExecutor
        );
        ReflectionTestUtils.setField(service, "vectorTableName", "rag_document");

        service.triggerAsyncRefresh();

        Mockito.verify(competitionRepository).findAll();
        Mockito.verify(jdbcTemplate).execute("TRUNCATE TABLE rag_document");
    }
}
