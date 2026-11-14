package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.domain.ChatHistory;
import com.mes.mesaichatbot.domain.ChatSession;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.dto.*;
import com.mes.mesaichatbot.service.LlmChatHistoryService;
import com.mes.mesaichatbot.service.LlmChatService;
import com.mes.mesaichatbot.service.LlmChatSessionService;
import com.mes.mesaichatbot.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * LLM과의 대화를 담당하는 RestController.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {
    // 의존성 자동 주입.
    private final UserService userService;
    private final LlmChatSessionService llmChatSessionService;
    private final LlmChatService llmChatService;
    private final LlmChatHistoryService llmChatHistoryService;

    /**
     * 사용자에게 질문을 받아 답변을 생성합니다.
     *
     * @param sessionId    대화 세션 ID.
     * @param userQuestion 사용자의 질문. ({@link UserQuestionDto})
     * @return {@link AskResponseDto} 응답 엔티티.
     */
    @PostMapping(value = "/sessions/{sessionId}/ask", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AskResponseDto> askToLlm(
            @PathVariable String sessionId, @RequestBody UserQuestionDto userQuestion) {
        // GlobalExceptionHandler파일안에 예외처리 권한 넘김
        // AccessDeniedException: 본인 소유의 대화방이 아닌 경우 403 Forbidden 반환
        // IllegalArgumentException: 잘못된 UUID 형식 등의 요청 인자인 경우 400 Bad Request 반환
        try {
            // 요청 시각 기록
            LocalDateTime requestedAt = LocalDateTime.now();

            // 사용자의 정보를 UserService에서 가져옴
            UserMember member = userService.getCurrentUserEntity();
            var chatResult = llmChatService.askToLlm(sessionId, member, requestedAt, userQuestion);

            // 성공 시 답변 객체 반환
            return ResponseEntity.ok().body(AskResponseDto.from(chatResult));
        } catch (ResponseStatusException e) {
            // 세션 미존재(404) 또는 빈 질문(400) 상태 코드 반환
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (IllegalStateException e) {
            log.error("LLM 응답이 비어 있거나 실패했습니다: {}", e.getMessage(), e);
            // 외부 LLM 응답이 비어 있거나 실패한 경우 502 Bad Gateway 반환
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    /**
     * 새로운 대화방을 만듭니다.
     *
     * @param chatSessionRequest 대화방 생성 요청 DTO.
     * @return {@link ChatSessionResponseDto} 응답 엔티티.
     */
    @PostMapping("/sessionrooms")
    public ResponseEntity<ChatSessionResponseDto> createChatSession(
            @RequestBody ChatSessionRequestDto chatSessionRequest) {
        LocalDateTime requestedAt = LocalDateTime.now();
        // GlobalExceptionHandler파일안에 예외처리 권한 넘김
        // IllegalArgumentException: 잘못된 요청 인자인 경우 400 Bad Request 반환
        // 사용자의 정보를 UserService에서 가져옴
        UserMember member = userService.getCurrentUserEntity();
        ChatSession session = llmChatSessionService.createChatSession(member, chatSessionRequest, requestedAt);
        // 생성 결과 반환
        return ResponseEntity.ok().body(ChatSessionResponseDto.from(session));
    }

    /**
     * 대화 내역을 불러옵니다.
     *
     * @param sessionId 대화방의 세션 ID.
     * @return List 형태의 대화 내역.
     */
    @GetMapping("/sessions/{sessionId}/histories")
    public ResponseEntity<List<ChatHistoryResponseDto>> getChatHistories(
            @PathVariable(name = "sessionId") String sessionId) {
        try {
            UserMember member = userService.getCurrentUserEntity();
            ChatSession session = llmChatSessionService.getChatSession(sessionId, member);
            List<ChatHistory> histories = llmChatHistoryService.getChatHistories(session, member);
            // 각 Message로부터 DTO 매핑 후 반환
            return ResponseEntity.ok(ChatHistoryResponseDto.from(histories));
        } catch (IllegalArgumentException e) {
            // 존재하지 않는 세션 ID인 경우 404 Not Found 반환 (GlobalExceptionHandler의 400과 구분하기 위해 유지)
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            // 본인의 대화방이 아닌 경우 403 Forbidden 반환
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }

    /**
     * 채팅 세션을 삭제합니다.
     *
     * @param sessionId 삭제 대상 세션 ID.
     * @return 응답 메시지.
     */
    @DeleteMapping(value = "/sessions/{sessionId}")
    public ResponseEntity<String> deleteChatSession(@PathVariable String sessionId) {
        try {
            // 현재 사용자 정보를 가져옴
            UserMember member = userService.getCurrentUserEntity();
            // 삭제 시도
            llmChatSessionService.deleteChatSessions(sessionId, member);
            return ResponseEntity.ok("삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            // 세션을 찾을 수 없을 경우 404 반환 (GlobalExceptionHandler의 400과 구분하기 위해 유지)
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            // 본인의 세션이 아니면 403 반환
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    /**
     * 모든 대화 세션을 불러옵니다.
     *
     * @return List 형태의 대화 세션 목록.
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionResponseDto>> getChatSessions() {
        // 대화 세션 불러오기
        return ResponseEntity.ok(ChatSessionResponseDto.from(
                llmChatSessionService.getChatSessions(userService.getCurrentUserEntity())));
    }
}
