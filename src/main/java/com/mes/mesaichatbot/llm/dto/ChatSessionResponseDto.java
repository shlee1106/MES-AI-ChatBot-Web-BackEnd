package com.mes.mesaichatbot.llm.dto;

import com.mes.mesaichatbot.domain.ChatSession;

import java.time.LocalDateTime;
import java.util.List;

public record ChatSessionResponseDto(String sessionId, Long companyId, Long employeeId, String sessionTitle,
                                     LocalDateTime createdAt) {
    public static ChatSessionResponseDto from(ChatSession session) {
        return new ChatSessionResponseDto(session.getSessionId(), session.getCompanyId().getCompanyId(), session.getEmployeeId().getEmployeeId(), session.getSessionTitle(), session.getCreatedAt());
    }

    public static List<ChatSessionResponseDto> from(List<ChatSession> sessions) {
        return sessions.parallelStream().map(ChatSessionResponseDto::from).toList();
    }
}
