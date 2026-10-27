package com.mes.mesaichatbot.llm.dto;

import com.mes.mesaichatbot.domain.ChatSession;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 대화 세션 응답 DTO
 *
 * @param sessionId    대화 세션 ID
 * @param companyId    회사 ID
 * @param employeeId   직원 ID
 * @param sessionTitle 세션 제목
 * @param createdAt    세션 생성 시간
 */
public record ChatSessionResponseDto(String sessionId, Long companyId, Long employeeId, String sessionTitle,
                                     LocalDateTime createdAt) {
    /**
     * 대화 세션 엔티티를 DTO로 변환합니다.
     *
     * @param session {@link ChatSession}
     * @return {@link ChatSessionResponseDto}
     */
    public static ChatSessionResponseDto from(ChatSession session) {
        return new ChatSessionResponseDto(session.getSessionId(), session.getCompanyId().getCompanyId(), session.getEmployeeId().getEmployeeId(), session.getSessionTitle(), session.getCreatedAt());
    }

    /**
     * 대화 세션 엔티티 리스트를 DTO 리스트로 변환합니다.
     *
     * @param sessions {@link ChatSession} 리스트
     * @return {@link ChatSessionResponseDto} 리스트
     */
    public static List<ChatSessionResponseDto> from(List<ChatSession> sessions) {
        return sessions.parallelStream().map(ChatSessionResponseDto::from).toList();
    }
}
