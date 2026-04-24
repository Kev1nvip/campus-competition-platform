package com.competition.backend.service;

public interface AiService {
    
    /**
     * 根据用户输入的 Prompt 提供智能推荐
     * 
     * @param prompt 用户描述（如：我的专业是计算机，擅长算法）
     * @return AI 生成的推荐建议
     */
    String recommend(String prompt);
}