package com.mes.mesaichatbot.llm.controller;

import com.mes.mesaichatbot.domain.ChatHistory;
import com.mes.mesaichatbot.domain.ChatSession;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.domain.user.service.UserService;
import com.mes.mesaichatbot.llm.dto.*;
import com.mes.mesaichatbot.llm.service.LlmChatService;
import com.mes.mesaichatbot.llm.service.LlmChatSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * LLM과의 대화를 담당하는 RestController.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {
    // 의존성 자동 주입.
    private final UserService userService;
    private final LlmChatSessionService llmChatSessionService;
    private final LlmChatService llmChatService;

    public ChatController(UserService userService, LlmChatSessionService llmChatSessionService, LlmChatService llmChatService) {
        this.userService = userService;
        this.llmChatSessionService = llmChatSessionService;
        this.llmChatService = llmChatService;
    }

    /**
     * 사용자에게 질문을 받아 답변을 생성합니다.
     *
     * @param sessionId    대화 세션 ID.
     * @param userQuestion 사용자의 질문. ({@link UserQuestionDto})
     * @return {@link ChatHistory}
     */
    @PostMapping(value = "/sessions/{sessionId}/ask", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AskResponseDto> askToLlm(
//            @RequestParam(name = "file", required = false) MultipartFile file,
            @PathVariable String sessionId, @RequestBody UserQuestionDto userQuestion) {
        try {
            // 요청 시각 기록 
            LocalDateTime requestedAt = LocalDateTime.now();

            // 사용자의 정보를 UserService에서 가져옴
            UserMember member = userService.getCurrentUserEntity();
            var chatResult = llmChatService.askToLlm(sessionId, member, requestedAt, userQuestion);

            // 성공 시 답변 객체 반환
            return ResponseEntity.ok().body(AskResponseDto.from(chatResult));
        } catch (NullPointerException | UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    /**
     * 새로운 대화방을 만듭니다.
     *
     * @return {@link ChatSession}
     */
    @PostMapping("/sessionrooms")
    public ResponseEntity<ChatSessionResponseDto> createChatSession(@RequestBody ChatSessionRequestDto chatSessionRequest) {
        LocalDateTime requestedAt = LocalDateTime.now();
        try {
            // 사용자의 정보를 UserService에서 가져옴
            UserMember member = userService.getCurrentUserEntity();
            ChatSession session = llmChatSessionService.createChatSession(member, chatSessionRequest, requestedAt);
            // 생성 결과 반환
            return ResponseEntity.ok().body(ChatSessionResponseDto.from(session));
        } catch (UsernameNotFoundException | NullPointerException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 대화 내역을 불러옵니다.
     *
     * @param sessionId 대화방의 세션 ID.
     * @return List 형태의 대화 내역.
     */
    @GetMapping("/sessions/{sessionId}/histories")
    public ResponseEntity<List<ChatHistoryResponseDto>> getChatHistories(@PathVariable(name = "sessionId") String sessionId) {
        try {
            UserMember member = userService.getCurrentUserEntity();
            var histories = llmChatSessionService.getChatHistories(sessionId, member);
            // 각 Message로부터 ChatHistoryNoSql 매핑, 모아서 반환
            return ResponseEntity.ok(ChatHistoryResponseDto.from(histories));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        } catch (NullPointerException | UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @DeleteMapping(value = "/sessions/{sessionId}")
    public ResponseEntity<String> deleteChatSession(@PathVariable String sessionId) {
        try {
            UserMember member = userService.getCurrentUserEntity();
            llmChatSessionService.deleteChatSessions(sessionId, member);
            return ResponseEntity.ok("삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (NullPointerException | UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }


    /**
     * 모든 대화 세션을 불러옵니다.
     *
     * @return List 형태의 대화 내역.
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionResponseDto>> getChatSessions() {
        try {
            return ResponseEntity.ok(ChatSessionResponseDto.from(llmChatSessionService.getChatSessions(userService.getCurrentUserEntity())));
        } catch (NullPointerException | UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
