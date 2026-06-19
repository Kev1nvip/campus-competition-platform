package com.competition.backend.service.impl;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.service.AiService;
import com.competition.backend.service.HybridContentRetriever;
import com.competition.backend.util.SecurityUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ChatLanguageModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final KnowledgeBaseServiceImpl knowledgeBaseService;
    private final JdbcTemplate jdbcTemplate;

    private HybridContentRetriever retriever;

    private final Map<Long, List<ChatMessage>> chatHistories = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY_PER_USER = 20;

    @PostConstruct
    public void init() {
        EmbeddingStoreContentRetriever vectorRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(10)
                .minScore(0.3)
                .build();
        this.retriever = new HybridContentRetriever(vectorRetriever, jdbcTemplate);
    }

    @Override
    public String recommend(String prompt) {
        String normalizedPrompt = prompt == null ? null : prompt.trim();
        if (normalizedPrompt == null || normalizedPrompt.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "prompt不能为空");
        }

        Long userId = SecurityUtil.getCurrentUserId();
        String preview = normalizedPrompt.length() > 80
                ? normalizedPrompt.substring(0, 80) + "..."
                : normalizedPrompt;
        log.info("AI recommend, userId={}, prompt={}", userId, preview);

        try {
            List<Content> retrieved = retriever.retrieve(Query.from(normalizedPrompt));
            StringBuilder context = new StringBuilder();
            if (retrieved != null && !retrieved.isEmpty()) {
                context.append("以下是数据库中相关的竞赛资料，请基于这些资料回答：\n");
                for (Content c : retrieved) {
                    context.append("- ").append(c.textSegment().text()).append("\n");
                }
            } else {
                context.append("（未检索到相关竞赛资料，请根据你的知识回答）");
            }

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new SystemMessage(
                    "你是一个校园学术竞赛推荐专家。\n" +
                    "你的任务是根据提供的竞赛背景资料，回答学生的问题或给出推荐建议。\n" +
                    "如果资料中没有相关信息，请诚实回答你不知道，不要胡编乱造。\n" +
                    "回答要专业、有条理，尽量使用 Markdown 格式。"
            ));

            List<ChatMessage> history = chatHistories.get(userId);
            if (history != null) {
                messages.addAll(history);
            }

            String userMsg = "【检索到的竞赛资料】\n" + context.toString() + "\n\n【学生提问】\n" + normalizedPrompt;
            messages.add(new UserMessage(userMsg));

            AiMessage response = chatModel.generate(messages).content();

            chatHistories.computeIfAbsent(userId, k -> new ArrayList<>());
            List<ChatMessage> userHistory = chatHistories.get(userId);
            userHistory.add(new UserMessage(normalizedPrompt));
            userHistory.add(new AiMessage(response.text()));
            while (userHistory.size() > MAX_HISTORY_PER_USER) {
                userHistory.remove(0);
            }

            log.info("AI recommend success, userId={}, responseLength={}", userId, response.text().length());
            return response.text();

        } catch (RuntimeException e) {
            Throwable root = rootCause(e);
            log.error("AI recommend failed, userId={}, rootCause={}", userId, root.getMessage(), e);
            if (root instanceof TimeoutException) {
                throw new BusinessException(ErrorCode.AI_UPSTREAM_TIMEOUT, "AI服务响应超时，请稍后重试");
            }
            String detail = root.getMessage() != null ? root.getMessage() : "未知错误";
            throw new BusinessException(ErrorCode.AI_RECOMMEND_FAILED, "AI推荐服务暂时不可用：" + detail);
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
