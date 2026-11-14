package com.mes.mesaichatbot.service;

import com.mes.mesaichatbot.domain.ChatHistory;
import com.mes.mesaichatbot.domain.ChatSession;
import com.mes.mesaichatbot.domain.Company;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.domain.UserRole;
import com.mes.mesaichatbot.dto.UserQuestionDto;
import com.mes.mesaichatbot.repository.ChatHistoryRepository;
import com.mes.mesaichatbot.repository.ChatSessionRepository;
import com.mes.mesaichatbot.repository.FileMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LlmChatService 질문 처리 테스트")
class LlmChatServiceTest {
    /**
     * 질문 길이 제한 (글자 수).
     */
    private static final int MAX_QUESTION_LENGTH = 1000;

    /**
     * 대화 내역 조회 서비스 mock.
     */
    private LlmChatHistoryService chatHistoryService;
    /**
     * ChatClient 빌더 제공자 mock.
     */
    private ObjectProvider<ChatClient.Builder> chatClientProvider;
    /**
     * VectorStore 제공자 mock.
     */
    private ObjectProvider<VectorStore> vectorStoreProvider;
    /**
     * 대화 세션 저장소 mock.
     */
    private ChatSessionRepository chatSessionRepository;
    /**
     * 대화 내역 저장소 mock.
     */
    private ChatHistoryRepository chatHistoryRepository;
    /**
     * 접근 가능한 파일 조회 저장소 mock.
     */
    private FileMetadataRepository fileMetadataRepository;
    /**
     * 테스트 대상 서비스.
     */
    private LlmChatService llmChatService;

    /**
     * LLM 호출에 사용할 ChatClient mock (깊은 스텁).
     */
    private ChatClient chatClient;
    /**
     * 요청 사용자 (사원번호 2000).
     */
    private UserMember member;
    /**
     * 요청 사용자의 소속 회사.
     */
    private Company company;

    @BeforeEach
    void setUp() {
        chatHistoryService = mock(LlmChatHistoryService.class);
        chatClientProvider = mock(ObjectProvider.class);
        vectorStoreProvider = mock(ObjectProvider.class);
        chatSessionRepository = mock(ChatSessionRepository.class);
        chatHistoryRepository = mock(ChatHistoryRepository.class);
        fileMetadataRepository = mock(FileMetadataRepository.class);

        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);

        company = new Company(1L, "MES", null);
        member = new UserMember(2000L, company, "이름", "encoded", "a@example.com", UserRole.ROLE_USER,
                                LocalDateTime.now());

        // 공통 스텁 (케이스마다 사용 여부가 달라 lenient 처리)
        lenient().when(chatClientProvider.getIfAvailable()).thenReturn(builder);
        lenient().when(vectorStoreProvider.getIfAvailable()).thenReturn(mock(VectorStore.class));
        lenient().when(builder.defaultSystem(anyString())).thenReturn(builder);
        lenient().when(builder.build()).thenReturn(chatClient);
        lenient().when(fileMetadataRepository.findAccessibleFileIds(any(), any())).thenReturn(List.of());
        lenient().when(chatHistoryService.getMessages(any(), any())).thenReturn(new ArrayList<>());
        lenient().when(chatSessionRepository.findBySessionId("s-1"))
                 .thenReturn(Optional.of(new ChatSession("s-1", member, "제목", LocalDateTime.now())));

        llmChatService = new LlmChatService(chatHistoryService, chatClientProvider, vectorStoreProvider,
                                            chatSessionRepository, chatHistoryRepository, fileMetadataRepository);
    }

    /**
     * LLM 답변 응답 스텁을 설정합니다.
     *
     * @param answer LLM 답변
     */
    private void stubLlmAnswer(String answer) {
        when(chatClient.prompt().messages(anyList()).user(anyString()).call().content()).thenReturn(answer);
    }

    @Test
    @DisplayName("대화 내역이 비어 있는 새 대화방의 첫 질문도 성공하는가?")
    void askToLlm_firstQuestionInEmptySession_success() {
        stubLlmAnswer("답변");

        ChatHistory result = llmChatService.askToLlm("s-1", member, LocalDateTime.now(),
                                                     new UserQuestionDto("첫 질문"));

        assertTrue(result.getMessage().startsWith("답변"));
        verify(chatHistoryRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("질문이 1001자이면 400 예외가 발생하고 LLM 호출 및 저장이 일어나지 않는가?")
    void askToLlm_questionOverMaxLength_badRequestWithoutLlmCall() {
        String longQuestion = "가".repeat(MAX_QUESTION_LENGTH + 1);
        UserQuestionDto question = new UserQuestionDto(longQuestion);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                                                 () -> llmChatService.askToLlm("s-1", member,
                                                                               LocalDateTime.now(), question));

        assertEquals(HttpStatus.BAD_REQUEST.value(), e.getStatusCode().value());
        verify(chatClient, never()).prompt();
        verify(chatHistoryRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("질문이 정확히 1000자이면 성공하는가?")
    void askToLlm_questionAtMaxLength_success() {
        stubLlmAnswer("답변");
        String question = "가".repeat(MAX_QUESTION_LENGTH);

        ChatHistory result = llmChatService.askToLlm("s-1", member, LocalDateTime.now(),
                                                     new UserQuestionDto(question));

        assertTrue(result.getMessage().startsWith("답변"));
        verify(chatHistoryRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("질문이 공백뿐이면 400 예외가 발생하는가?")
    void askToLlm_blankQuestion_badRequest() {
        UserQuestionDto question = new UserQuestionDto("   ");

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                                                 () -> llmChatService.askToLlm("s-1", member,
                                                                               LocalDateTime.now(), question));

        assertEquals(HttpStatus.BAD_REQUEST.value(), e.getStatusCode().value());
    }

    @Test
    @DisplayName("다른 사용자의 세션에 질문하면 AccessDeniedException이 발생하는가?")
    void askToLlm_otherUsersSession_accessDenied() {
        UserMember otherMember = new UserMember(3000L, company, "타인", "encoded", "b@example.com",
                                                UserRole.ROLE_USER, LocalDateTime.now());
        when(chatSessionRepository.findBySessionId("s-1"))
                .thenReturn(Optional.of(new ChatSession("s-1", otherMember, "제목", LocalDateTime.now())));

        assertThrows(AccessDeniedException.class,
                     () -> llmChatService.askToLlm("s-1", member, LocalDateTime.now(),
                                                   new UserQuestionDto("질문")));
    }

    @Test
    @DisplayName("세션이 없으면 404 예외가 발생하는가?")
    void askToLlm_sessionNotFound_notFound() {
        when(chatSessionRepository.findBySessionId("s-1")).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                                                 () -> llmChatService.askToLlm("s-1", member,
                                                                               LocalDateTime.now(),
                                                                               new UserQuestionDto("질문")));

        assertEquals(HttpStatus.NOT_FOUND.value(), e.getStatusCode().value());
    }
}