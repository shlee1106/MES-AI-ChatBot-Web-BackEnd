package com.mes.mesaichatbot.service;

import com.mes.mesaichatbot.domain.ChatHistory;
import com.mes.mesaichatbot.domain.ChatSession;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.dto.UserQuestionDto;
import com.mes.mesaichatbot.repository.ChatHistoryRepository;
import com.mes.mesaichatbot.repository.ChatSessionRepository;
import com.mes.mesaichatbot.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class LlmChatService {
    /**
     * 사용자의 질문이 들어갈 템플릿.
     * %s가 사용자의 질문으로 바뀔 것입니다.
     */
    private static final String USER_QUESTION_TEMPLATE = """
            <!-- START OF USER PROMPT -->
            %s
            <!-- END OF USER PROMPT -->""";
    /**
     * AI의 답변이 들어갈 템플릿.
     * %s가 AI의 답변으로 바뀔 것입니다.
     */
    private static final String ASSISTANT_ANSWER_TEMPLATE = """
            <!-- START OF ASSISTANT RESPONSE -->
            %s
            <!-- END OF ASSISTANT RESPONSE -->""";
    /**
     * LLM의 시스템 프롬프트.
     */
    private static final String SYSTEM_PROMPT = """
            <!-- START OF SYSTEM PROMPT -->
            당신은 한 회사의 비서입니다.
            당신의 목표는 제공된 자료를 바탕으로 사용자의 질문에 답변하는 것입니다.
            외부 지식이 아닌 제공된 자료 내에서만 답변하십시오.
            
            # 언어
            사용자가 명시하지 않는 한, 모든 답변은 한국어여야 합니다.
            
            # 간결함 유지
            후속 질문은 하지 마시고, 답변은 3줄 내외로 하십시오.
            <!-- END OF SYSTEM PROMPT -->""";
    private final LlmChatHistoryService chatHistoryService;
    private final ObjectProvider<ChatClient.Builder> chatClientProvider;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatHistoryRepository chatHistoryRepository;
    private final FileMetadataRepository fileMetadataRepository;

    /**
     * ChatClient를 얻습니다.
     *
     * @return {@link ChatClient}
     * @throws UnsupportedOperationException ChatClient를 사용할 수 없을 때 발생
     */
    private ChatClient getChatClient() throws UnsupportedOperationException {
        ChatClient.Builder builder = chatClientProvider.getIfAvailable();
        if (builder == null) {
            throw new UnsupportedOperationException("ChatClient를 사용할 수 없습니다.");
        }
        return builder.defaultSystem(SYSTEM_PROMPT).build();
    }

    /**
     * VectorStore를 얻습니다.
     *
     * @return {@link VectorStore}
     * @throws UnsupportedOperationException VectorStore를 사용할 수 없을 때 발생
     */
    private VectorStore getVectorStore() throws UnsupportedOperationException {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            throw new UnsupportedOperationException("VectorStore를 사용할 수 없습니다.");
        }
        return vectorStore;
    }

    /**
     * 사용자의 질문을 받아 RAG 기반으로 LLM 답변을 생성하고 대화 내역을 저장합니다.
     *
     * @param sessionId    대화 세션 ID
     * @param member       요청 사용자 엔티티
     * @param requestedAt  요청 시각
     * @param userQuestion 사용자의 질문 DTO
     * @return 생성된 AI 답변 {@link ChatHistory}
     * @throws IllegalArgumentException UUID 형식이 올바르지 않을 경우 발생 (400)
     * @throws AccessDeniedException    본인 소유의 대화방이 아닐 경우 발생 (403)
     * @throws ResponseStatusException  세션을 찾을 수 없거나(404) 질문이 비어 있을 경우(400) 발생
     * @throws IllegalStateException    LLM 응답이 비어 있을 경우 발생 (502)
     */
    @Transactional
    public ChatHistory askToLlm(String sessionId, UserMember member, LocalDateTime requestedAt,
                                UserQuestionDto userQuestion)
            throws IllegalArgumentException, AccessDeniedException, ResponseStatusException, IllegalStateException {
        ChatClient chatClient = getChatClient();
        VectorStore vectorStore = getVectorStore();

        // DB에서 대화 세션 조회 및 본인 소유 세션인지 권한 검증 (타인 세션이면 403 반환)
        ChatSession session = chatSessionRepository.findBySessionId(sessionId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!session.getEmployeeId().equals(member)) {
            throw new AccessDeniedException("본인의 대화방에만 질문할 수 있습니다.");
        }

        // 세션 ID로 대화방 조회
        List<Message> messages = chatHistoryService.getMessages(session, member);

        // 대화방을 찾을 수 없는 경우 Not Found
        if (messages == null || messages.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        // 사용자의 질문이 비어 있으면 Bad Request
        if (userQuestion.message() == null || userQuestion.message().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        // 접근 가능한 문서를 전부 가져옴
        List<String> accessibleDocumentIds =
                fileMetadataRepository.findAccessibleFileIds(member.getCompany(), member);

        // 사용자의 질문을 구분자로 감싸기
        String formattedQuestion = USER_QUESTION_TEMPLATE.formatted(userQuestion.message());

        // LLM 요청 객체 정의
        ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt();

        // 접근 가능한 문서가 없으면 RAG 검색을 하지 않음
        // 빈 목록으로 검색하면 필터 쿼리가 잘못되어 OpenSearch 검색이 실패함
        if (accessibleDocumentIds.isEmpty()) {
            log.debug("접근 가능한 문서가 없어 RAG 검색을 생략합니다.");
        } else {
            // 문서 RAG 검색 도구 생성
            // 접근 권한이 있는 문서만 포함
            RetrievalAugmentationAdvisor ragAdvisor = RetrievalAugmentationAdvisor.builder().documentRetriever(
                    VectorStoreDocumentRetriever.builder()
                                                .similarityThreshold(0.5)
                                                .vectorStore(vectorStore)
                                                .filterExpression(
                                                        new FilterExpressionBuilder()
                                                                .in("fileId",
                                                                    accessibleDocumentIds.toArray())
                                                                .build()).build()).build();
            requestSpec = requestSpec.advisors(ragAdvisor);
        }

        // LLM 요청
        ChatClient.CallResponseSpec response = requestSpec.messages(messages).user(formattedQuestion).call();

        // LLM의 답변 임시 저장
        String llmAnswer = response.content();
        // 답변 시각 기록
        LocalDateTime respondedAt = LocalDateTime.now();

        // LLM의 답변이 비어 있으면 예외
        if (llmAnswer == null || llmAnswer.isBlank()) {
            log.error("LLM이 답변에 실패했습니다. 답변이 비어 있습니다.");
            throw new IllegalStateException("LLM이 답변에 실패했습니다. 답변이 비어 있습니다.");
        }

        // 답변 뒤에 면책 추가
        llmAnswer = llmAnswer.concat("""
                                             
                                             
                                             해당 정보는 정확하지 않을 수 있으며, 사측에서는 해당 답변으로 인해 \
                                             발생하는 모든 피해에 대해 어떠한 책임도 지지 않습니다.""");

        // 대화 내역 기록
        messages.add(UserMessage.builder()
                                .metadata(Map.of("recordedAt", requestedAt))
                                .text(formattedQuestion)
                                .build());
        messages.add(AssistantMessage.builder()
                                     .properties(Map.of("recordedAt", respondedAt))
                                     .content(ASSISTANT_ANSWER_TEMPLATE.formatted(llmAnswer))
                                     .build());

        // Ai 답변 결과를 조립 (null = 기본값 사용)
        ChatHistory chatResult =
                new ChatHistory(null, member, session, llmAnswer, respondedAt, "assistant");

        // 유저 질문 결과 조립
        ChatHistory userHistory =
                new ChatHistory(null, member, session, userQuestion.message(), requestedAt, "user");

        // 대화내역 저장
        chatHistoryRepository.saveAll(List.of(userHistory, chatResult));

        return chatResult;
    }
}
