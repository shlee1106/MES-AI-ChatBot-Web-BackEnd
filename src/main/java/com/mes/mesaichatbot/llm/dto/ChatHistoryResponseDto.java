package com.mes.mesaichatbot.llm.dto;

import com.mes.mesaichatbot.domain.ChatHistory;

import java.time.LocalDateTime;
import java.util.List;

public record ChatHistoryResponseDto(String chatId, Long companyId, Long employeeId, String sessionId, String message,
                                     LocalDateTime recordedAt, String role) {
    public static ChatHistoryResponseDto from(ChatHistory history) {
        return new ChatHistoryResponseDto(history.getChatId(), history.getCompanyId().getCompanyId(), history.getEmployeeId().getEmployeeId(), history.getSessionId().getSessionId(), history.getMessage(), history.getRecordedAt(), history.getRole());
    }

    public static List<ChatHistoryResponseDto> from(List<ChatHistory> histories) {
        return histories.parallelStream().map(ChatHistoryResponseDto::from).toList();
    }
}