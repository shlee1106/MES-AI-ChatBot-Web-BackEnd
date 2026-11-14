package com.mes.mesaichatbot.service;

import com.mes.mesaichatbot.domain.ChatHistory;
import com.mes.mesaichatbot.domain.ChatSession;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 대화 내역을 관리합니다.
 */
@Service
@RequiredArgsConstructor
public class LlmChatHistoryService {
    //인터페이스 메소드 사용을 위한 객체생성
    private final ChatHistoryRepository chatHistoryRepository;

    /**
     * 세션의 모든 대화 기록(LLM용)을 가져옵니다.
     *
     * @param member  사용자.
     * @param session 세션.
     * @return {@link Message} 리스트.
     * @throws AccessDeniedException 타인의 대화 기록에는 접근할 수 없습니다!
     */
    @Transactional(readOnly = true)
    public List<Message> getMessages(ChatSession session, UserMember member)
            throws AccessDeniedException {
        if (!session.getEmployeeId().equals(member)) {
            throw new AccessDeniedException("타인의 대화 기록에는 접근할 수 없습니다!");
        }

        // 세션의 모든 대화 기록을 가져옴
        List<ChatHistory> histories = chatHistoryRepository.findAllBySessionIdOrderByRecordedAtAsc(session);

        // 각 대화 기록을 Message 객체로 변환
        return histories.stream()
                        .map(history -> {
                            // 해당 대화 기록의 사용자 ID가 현재 로그인한 사용자와 일치하지 않으면 접근 거부
                            if (!history.getEmployeeId().equals(member)) {
                                throw new AccessDeniedException("타인의 대화 기록에는 접근할 수 없습니다!");
                            }

                            if (history.getRole().equals("user")) {
                                return UserMessage.builder().text(history.getMessage()).build();
                            } else {
                                return AssistantMessage.builder().content(history.getMessage()).build();
                            }
                        })
                        .collect(Collectors.toList());
    }

    /**
     * 특정 대화방의 대화 내역(레코드)을 조회합니다.
     *
     * @param session 조회할 대화 세션
     * @param member  현재 로그인한 사용자 엔티티
     * @return 시간순으로 정렬된 {@link ChatHistory} 리스트
     * @throws IllegalArgumentException 존재하지 않는 세션 ID일 경우 발생 (404)
     * @throws AccessDeniedException    본인 소유의 대화방이 아닐 경우 발생 (403)
     */
    @Transactional(readOnly = true)
    public List<ChatHistory> getChatHistories(ChatSession session, UserMember member)
            throws IllegalArgumentException, AccessDeniedException {
        // 세션 존재 여부 먼저 확인 (존재하지 않으면 404 처리를 위해 예외 발생)
        if (session == null) {
            throw new IllegalArgumentException("세션을 찾을 수 없습니다.");
        }

        // 대화 내역이 0건인 빈 방이라도 타인의 방이면 조회할 수 없도록 세션 소유자(회사번호·사원번호) 검증
        if (!session.getEmployeeId().equals(member)) {
            throw new AccessDeniedException("본인의 대화방만 조회할 수 있습니다.");
        }

        // 해당 세션의 대화 내역을 시간 오름차순으로 조회
        List<ChatHistory> histories = chatHistoryRepository.findAllBySessionIdOrderByRecordedAtAsc(session);

        // 대화 내역 중 타인의 내역이 포함된 경우 이중 검증 (403 반환)
        if (histories.stream().anyMatch(
                chatHistory -> !chatHistory.getEmployeeId().equals(member))) {
            throw new AccessDeniedException("본인의 대화방만 조회할 수 있습니다.");
        }

        return histories;
    }
}
