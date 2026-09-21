package com.mes.mesaichatbot.llm.dto;

import java.time.LocalDateTime;

public record AskResponseDto(
        String chatId,
        Long companyId,
        Long employeeId,
        String sessionId,
        String message,
        LocalDateTime recordedAt,
        String role
) {
}
