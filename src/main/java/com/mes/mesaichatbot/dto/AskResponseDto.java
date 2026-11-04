package com.mes.mesaichatbot.dto;

import com.mes.mesaichatbot.domain.ChatHistory;

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
    /**
     * 해당 ChatHistory 객체로부터 DTO를 생성합니다.
     *
     * @return {@link AskResponseDto}
     */
    public static AskResponseDto from(ChatHistory history) {
        return new AskResponseDto(history.getChatId(), history.getCompanyId().getCompanyId(),
                                  history.getEmployeeId().getEmployeeId(), history.getSessionId().getSessionId(),
                                  history.getMessage(), history.getRecordedAt(), history.getRole());
    }
}
