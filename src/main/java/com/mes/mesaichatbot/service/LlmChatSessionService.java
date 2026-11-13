package com.mes.mesaichatbot.service;

import com.mes.mesaichatbot.domain.ChatSession;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.dto.ChatSessionRequestDto;
import com.mes.mesaichatbot.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
        ChatSession chatSession = new ChatSession(sessionUuid.toString(), member, title, requestedAt);
        chatSessionRepository.save(chatSession);
        return chatSession;
    }

    /**
     * 현재 사용자의 대화방 목록 조회
     *
     * @param member 사용자 정보
     * @return 대화방 목록
     * @throws NullPointerException      사용자 정보가 null인 경우
     * @throws UsernameNotFoundException 사용자 정보가 존재하지 않는 경우
     */
    @Transactional(readOnly = true)
    public List<ChatSession> getChatSessions(UserMember member) throws NullPointerException, UsernameNotFoundException {
        return chatSessionRepository.findAllByEmployeeId(member);
    }

    /**
     * 세션 ID로 대화방을 조회합니다.
     *
     * @param sessionId 대화방 ID
     * @param member    사용자 정보
     * @return 대화방 정보
     * @throws IllegalArgumentException 대화방 ID가 유효하지 않을 경우
     * @throws AccessDeniedException    본인의 대화방이 아닐 경우
     */
    @Transactional(readOnly = true)
    public ChatSession getChatSession(String sessionId,
                                      UserMember member) throws IllegalArgumentException, AccessDeniedException {
        // 세션 ID로 대화방 조회
        ChatSession session = chatSessionRepository.findById(sessionId)
                                                   .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));

        // 본인 소유의 대화방인지 검증 (회사번호 + 사원번호 일치 확인)
        if (!session.getEmployeeId().equals(member)) {
            throw new AccessDeniedException("본인의 대화방만 조회할 수 있습니다.");
        }

        return session;
    }

    @Transactional
    public void deleteChatSessions(String sessionId,
                                   UserMember member) throws IllegalArgumentException, AccessDeniedException {
        ChatSession session = chatSessionRepository.findById(sessionId)
                                                   .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));

        // 본인 소유의 대화방인지 검증 (회사번호 + 사원번호 일치 확인)
        if (!session.getEmployeeId().equals(member)) {
            throw new AccessDeniedException("본인의 대화방만 삭제할 수 있습니다.");
        }

        // 대화 세션 삭제
        chatSessionRepository.delete(session);
    }
}
