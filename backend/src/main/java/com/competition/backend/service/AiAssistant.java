package com.competition.backend.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface AiAssistant {

    @SystemMessage({
        "你是一个校园学术竞赛推荐专家。",
        "你的任务是根据提供的竞赛背景资料，回答学生的问题或给出推荐建议。",
        "如果资料中没有相关信息，请诚实回答你不知道，不要胡编乱造。",
        "回答要专业、有条理，尽量使用 Markdown 格式。"
    })
    String chat(@UserMessage String userMessage);
}