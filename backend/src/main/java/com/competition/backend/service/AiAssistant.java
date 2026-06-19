package com.competition.backend.service;

public interface AiAssistant {
    String chat(Long memoryId, String userMessage);
}