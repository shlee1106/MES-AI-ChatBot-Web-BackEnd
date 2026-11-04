package com.mes.mesaichatbot.llm.service;

import com.mes.mesaichatbot.domain.ChatHistory;
import com.mes.mesaichatbot.domain.ChatSession;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.llm.dto.ChatSessionRequestDto;
import com.mes.mesaichatbot.repository.ChatHistoryRepository;
import com.mes.mesaichatbot.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmChatSessionService {
    /**
     * 무작위 단어 목록.
     * 대화방 이름이 제공되지 않았을 때
     * 조합되어 새로운 제목을 만듭니다.
     */
    private static final String[] WORDS = {"apple", "river", "cloud", "mountain", "garden", "ocean", "silver", "paper", "coffee", "orange", "breeze", "castle", "mirror", "rabbit", "pencil", "sunset", "flower", "camera", "desert", "pillow", "guitar", "bridge", "shadow", "lantern", "winter", "summer", "autumn", "spring", "dragon", "crystal", "forest", "candle", "whisper", "blanket", "marble", "window", "planet", "anchor", "compass", "harbor", "feather", "meadow", "thistle", "rainbow", "tunnel", "pocket", "button", "helmet", "violin", "desire", "memory", "picture", "bottle", "basket", "rocket", "island", "valley", "canyon", "thunder", "sunrise", "moonlight", "starlight", "snowflake", "raindrop", "seashell", "pineapple", "strawberry", "blueberry", "watermelon", "notebook", "keyboard", "monitor", "library", "factory", "station", "airport", "village", "market", "museum", "theater", "concert", "festival", "journey", "adventure", "treasure", "diamond", "emerald", "sapphire", "amethyst"};
    private final ChatSessionRepository chatSessionRepository;
    private final LlmChatHistoryService llmChatHistoryService;
    private final ChatHistoryRepository chatHistoryRepository;
    private final SecureRandom rng = new SecureRandom();

    @Transactional
    public ChatSession createChatSession(UserMember member, ChatSessionRequestDto chatSessionRequest,
                                         LocalDateTime requestedAt)
            throws IllegalArgumentException {
        // 대화방의 ID 생성
        UUID sessionUuid = UUID.randomUUID();

        // Title이 있으면 그대로 사용, 아니면 무작위 단어 3개 배합
        String title = chatSessionRequest.title() != null ? chatSessionRequest.title() : "%s-%s-%s".formatted(
                WORDS[rng.nextInt(0, WORDS.length)], WORDS[rng.nextInt(0, WORDS.length)],
                WORDS[rng.nextInt(0, WORDS.length)]);

        // 대화 내역 Map에 새로 등록
        llmChatHistoryService.getChatHistories().put(sessionUuid, new ArrayList<>());

        ChatSession chatSession = new ChatSession(sessionUuid.toString(), member, title, requestedAt);

        chatSessionRepository.save(chatSession);
        return chatSession;
    }

    @Transactional(readOnly = true)
    public List<ChatSession> getChatSessions(UserMember member) throws NullPointerException, UsernameNotFoundException {
        return chatSessionRepository.findAllByEmployeeId(member);
    }

    /**
     * 특정 대화방의 대화 내역을 조회합니다.
     *
     * @param sessionId 조회할 대화 세션 ID
     * @param member    현재 로그인한 사용자 엔티티
     * @return 시간순으로 정렬된 {@link ChatHistory} 리스트
     * @throws IllegalArgumentException 존재하지 않는 세션 ID일 경우 발생 (404)
     * @throws AccessDeniedException    본인 소유의 대화방이 아닐 경우 발생 (403)
     */
    @Transactional(readOnly = true)
    public List<ChatHistory> getChatHistories(String sessionId, UserMember member)
            throws IllegalArgumentException, AccessDeniedException {
        // 세션 존재 여부 먼저 확인 (존재하지 않으면 404 처리를 위해 예외 발생)
        ChatSession session = chatSessionRepository.findBySessionId(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("세션을 찾을 수 없습니다.");
        }

        // 대화 내역이 0건인 빈 방이라도 타인의 방이면 조회할 수 없도록 세션 소유자(회사번호·사원번호) 검증
        if (!Objects.equals(session.getEmployeeId().getEmployeeId(), member.getEmployeeId())
                || !Objects.equals(session.getCompanyId().getCompanyId(), member.getCompanyId().getCompanyId())) {
            throw new AccessDeniedException("본인의 대화방만 조회할 수 있습니다.");
        }

        // 해당 세션의 대화 내역을 시간 오름차순으로 조회
        List<ChatHistory> histories = chatHistoryRepository.findAllBySessionIdOrderByRecordedAtAsc(session);

        // 대화 내역 중 타인의 내역이 포함된 경우 이중 검증 (403 반환)
        if (histories.stream().anyMatch(
                chatHistory -> !Objects.equals(chatHistory.getEmployeeId().getEmployeeId(), member.getEmployeeId()))) {
            throw new AccessDeniedException("본인의 대화방만 조회할 수 있습니다.");
        }

        return histories;
    }

    @Transactional
    public void deleteChatSessions(String sessionId,
                                   UserMember member) throws IllegalArgumentException, AccessDeniedException {
        ChatSession session = chatSessionRepository.findById(sessionId)
                                                   .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));

        // 본인 소유의 대화방인지 검증 (회사번호 + 사원번호 일치 확인)
        if (!session.getEmployeeId().getEmployeeId().equals(member.getEmployeeId()) || !session.getCompanyId()
                                                                                               .getCompanyId()
                                                                                               .equals(member.getCompanyId()
                                                                                                             .getCompanyId())) {
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
