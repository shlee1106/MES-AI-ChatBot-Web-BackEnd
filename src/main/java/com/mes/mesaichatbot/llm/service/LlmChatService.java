package com.mes.mesaichatbot.llm.service;

import com.mes.mesaichatbot.domain.ChatHistory;
import com.mes.mesaichatbot.domain.ChatSession;
import com.mes.mesaichatbot.domain.Company;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.llm.dto.UserQuestionDto;
import com.mes.mesaichatbot.repository.ChatHistoryRepository;
import com.mes.mesaichatbot.repository.ChatSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
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
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatHistoryRepository chatHistoryRepository;

    public LlmChatService(LlmChatHistoryService chatHistoryService, ChatClient.Builder chatClient, VectorStore vectorStore, ChatSessionRepository chatSessionRepository, ChatHistoryRepository chatHistoryRepository) {
        this.chatClient = chatClient.defaultSystem(SYSTEM_PROMPT).build();
        this.chatHistoryService = chatHistoryService;
        this.vectorStore = vectorStore;
        this.chatSessionRepository = chatSessionRepository;
        this.chatHistoryRepository = chatHistoryRepository;
    }

    @Transactional
    public ChatHistory askToLlm(String sessionId, UserMember member, LocalDateTime requestedAt, UserQuestionDto userQuestion) throws IllegalArgumentException, ResponseStatusException, IllegalStateException {
        Company company = member.getCompanyId();

        // URL 파라미터의 UUID 파싱 시도
        UUID sessionUuid = UUID.fromString(sessionId);

        // UUID로 메시지 목록 불러오기
        List<Message> messages = chatHistoryService.getChatHistories().get(sessionUuid);

        // UUID로 목록을 찾을 수 없는 경우 Not Found
        if (messages == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        // 사용자의 질문이 비어 있으면 Bad Request
        if (userQuestion.message() == null || userQuestion.message().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        // 사용자의 질문을 구분자로 감싸기
        String formattedQuestion = USER_QUESTION_TEMPLATE.formatted(userQuestion.message());

        // 문서 RAG 검색 도구 생성
        RetrievalAugmentationAdvisor ragAdvisor = RetrievalAugmentationAdvisor.builder().documentRetriever(VectorStoreDocumentRetriever.builder().similarityThreshold(0.5).vectorStore(vectorStore).build()).build();
        // LLM 요청 객체 정의 및 요청
        ChatClient.CallResponseSpec response = chatClient.prompt().advisors(ragAdvisor).messages(messages).user(formattedQuestion).call();

        // 파일 업로드가 있을 경우의 코드. API 규격을 지키기 위해 제거.
//        if (file == null || file.getContentType() == null || file.isEmpty()) {
//            response = chatClient.prompt().advisors(ragAdvisor).user(formattedQuestion).call();
//        } else {
//            try {
//                MimeType fileType = MimeTypeUtils.parseMimeType(file.getContentType());
//                Resource fileData = new ByteArrayResource(file.getBytes());
//                response = chatClient.prompt().advisors(ragAdvisor).user(u -> u.media(fileType, fileData).text(formattedQuestion)).call();
//            } catch (IOException | InvalidMimeTypeException e) {
//                return ResponseEntity.badRequest().build();
//            }
//        }

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
        llmAnswer = llmAnswer.concat("\n\n해당 정보는 정확하지 않을 수 있으며, 사측에서는 해당 답변으로 인해 발생하는 모든 피해에 대해 어떠한 책임도 지지 않습니다.");

        // 대화 내역 기록
        messages.add(UserMessage.builder().metadata(Map.of("recordedAt", requestedAt)).text(formattedQuestion).build());
        messages.add(AssistantMessage.builder().properties(Map.of("recordedAt", respondedAt)).content(ASSISTANT_ANSWER_TEMPLATE.formatted(llmAnswer)).build());

        // Ai 답변 결과를 조립 (null = 기본값 사용)
        ChatSession session = chatSessionRepository.findBySessionId(sessionId);
        ChatHistory chatResult = new ChatHistory(null, company, member, session, llmAnswer, respondedAt, "assistant");

        // 유저 질문 결과 조립
        ChatHistory userHistory = new ChatHistory(null, company, member, session, userQuestion.message(), requestedAt, "user");

        // 대화내역 저장
        chatHistoryRepository.saveAll(List.of(userHistory, chatResult));

        return chatResult;
    }
}
