package com.mes.mesaichatbot.llm.service;

import com.mes.mesaichatbot.domain.ChatSession;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.repository.ChatSessionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LlmChatSessionService {
    private final ChatSessionRepository chatSessionRepository;

    public List<ChatSession> getChatSessions(UserMember member) throws NullPointerException, UsernameNotFoundException {
        return chatSessionRepository.findAllByEmployeeId(member);
    }

    @Transactional
    public void deleteChatSessions(String sessionId) throws IllegalArgumentException {
        if (chatSessionRepository.existsById(sessionId)) {
            chatSessionRepository.deleteById(sessionId);
        } else {
            throw new IllegalArgumentException("세션을 찾을 수 없습니다.");
        }
    }
}
