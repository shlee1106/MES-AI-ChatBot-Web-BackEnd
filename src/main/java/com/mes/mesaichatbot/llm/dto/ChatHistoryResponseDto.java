package com.mes.mesaichatbot.llm.dto;

import com.mes.mesaichatbot.domain.ChatHistory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 대화 기록 응답 DTO
 *
 * @param chatId     대화 ID
 * @param companyId  회사 ID
 * @param employeeId 직원 ID
 * @param sessionId  세션 ID
 * @param message    메시지
 * @param recordedAt 기록된 시간
 * @param role       역할(user, assistant)
 */
public record ChatHistoryResponseDto(String chatId, Long companyId, Long employeeId, String sessionId, String message,
                                     LocalDateTime recordedAt, String role) {
    /**
     * 대화 기록 엔티티를 DTO로 변환합니다.
     *
     * @param history {@link ChatHistory}
     * @return {@link ChatHistoryResponseDto}
     */
    public static ChatHistoryResponseDto from(ChatHistory history) {
        return new ChatHistoryResponseDto(history.getChatId(), history.getCompanyId().getCompanyId(), history.getEmployeeId().getEmployeeId(), history.getSessionId().getSessionId(), history.getMessage(), history.getRecordedAt(), history.getRole());
    }

    /**
     * 대화 기록 엔티티 리스트를 DTO 리스트로 변환합니다.
     *
     * @param histories {@link ChatHistory}
     * @return {@link ChatHistoryResponseDto}
     */
    public static List<ChatHistoryResponseDto> from(List<ChatHistory> histories) {
        return histories.parallelStream().map(ChatHistoryResponseDto::from).toList();
    }
}