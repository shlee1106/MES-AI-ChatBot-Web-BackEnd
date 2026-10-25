package com.mes.mesaichatbot.llm.service;

import com.mes.mesaichatbot.domain.ChatSession;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.repository.ChatSessionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmChatSessionService {
    private final ChatSessionRepository chatSessionRepository;
    private final LlmChatHistoryService llmChatHistoryService;

    public List<ChatSession> getChatSessions(UserMember member) throws NullPointerException, UsernameNotFoundException {
        return chatSessionRepository.findAllByEmployeeId(member);
    }

    @Transactional
    public void deleteChatSessions(String sessionId, UserMember member) throws IllegalArgumentException, AccessDeniedException {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));

        // 본인 소유의 대화방인지 검증 (회사번호 + 사원번호 일치 확인)
        if (!Objects.equals(session.getEmployeeId().getEmployeeId(), member.getEmployeeId()) ||
                !Objects.equals(session.getCompanyId().getCompanyId(), member.getCompanyId().getCompanyId())) {
            throw new AccessDeniedException("본인의 대화방만 삭제할 수 있습니다.");
        }

        chatSessionRepository.delete(session);

        // 인메모리 대화 내역 캐시에서도 제거
        try {
            llmChatHistoryService.getChatHistories().remove(UUID.fromString(sessionId));
        } catch (IllegalArgumentException e) {
            log.warn("제거할 세션 항목이 없습니다.", e);
            // UUID 형식이 아닌 경우 캐시 제거 생략
        }
    }
}