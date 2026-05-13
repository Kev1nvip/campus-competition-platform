package com.competition.backend.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AiConfig {

    @Value("${ai.siliconflow.api-key}")
    private String apiKey;

    @Value("${ai.siliconflow.base-url}")
    private String baseUrl;

    @Value("${ai.siliconflow.chat-model}")
    private String chatModelName;

    @Value("${ai.siliconflow.embedding-model}")
    private String embeddingModelName;

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(chatModelName)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 对接硅基流动的向量化模型 (BGE-M3)
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(embeddingModelName)
                .build();
    }

    /**
     * PGVector 向量数据库存储
     * 映射到数据库中的 rag_document 表
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        String cleanUrl = jdbcUrl.replace("jdbc:postgresql://", "");
        String hostPort = cleanUrl.split("/")[0];
        String host = hostPort.split(":")[0];
        int port = Integer.parseInt(hostPort.split(":")[1]);
        String database = cleanUrl.split("/")[1];

        return PgVectorEmbeddingStore.builder()
                .host(host)
                .port(port)
                .database(database)
                .user(dbUser)
                .password(dbPassword)
                .table("rag_document")
                .dimension(1024)
                .build();
    }
}
