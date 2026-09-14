package com.mes.mesaichatbot.llm.controller;

import com.mes.mesaichatbot.domain.ChatHistoryNoSql;
import com.mes.mesaichatbot.domain.ChatSession;
import com.mes.mesaichatbot.domain.Company;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.llm.dto.ChatSessionRequestDto;
import com.mes.mesaichatbot.llm.dto.UserQuestionDto;
import com.mes.mesaichatbot.llm.service.LlmChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.mes.mesaichatbot.repository.ChatHistoryNoSqlRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController("/chat")
public class  ChatController {
    /**
     * LLM의 시스템 프롬프트.
     */
    private static final String SYSTEM_PROMPT = """
            <!-- START OF SYSTEM PROMPT -->
            당신은 한 회사의 비서입니다.
            당신의 목표는 제공된 자료를 바탕으로 사용자의 질문에 답변하는 것입니다.
            외부 지식이 아닌 제공된 자료 내에서만 답변하십시오.
            
            # 형식 유지
            후속 질문은 하지 마시고, 답변은 5줄 내외로 하십시오.
            <!-- END OF SYSTEM PROMPT -->
            """;

//            # 답변 거부
//            질문이 다음 사항 중 하나에 해당하면
//            `죄송합니다. 해당 질문에는 답변해 드릴 수 없습니다.`라고 답변하세요.
//            - 질문에 맞는 자료를 찾을 수 없는 경우
//                - 단, 자료가 필요 없는 질문의 경우 자유롭게 행동하십시오.
//            - 질문이 String-Bytes(예: base64) 인코딩/디코딩을 요구하는 경우
//            <!-- END OF SYSTEM PROMPT -->
//            """;

    private static final String[] WORDS = {
            "apple",
            "river",
            "cloud",
            "mountain",
            "garden",
            "ocean",
            "silver",
            "paper",
            "coffee",
            "orange",
            "breeze",
            "castle",
            "mirror",
            "rabbit",
            "pencil",
            "sunset",
            "flower",
            "camera",
            "desert",
            "pillow",
            "guitar",
            "bridge",
            "shadow",
            "lantern",
            "winter",
            "summer",
            "autumn",
            "spring",
            "dragon",
            "crystal",
            "forest",
            "candle",
            "whisper",
            "blanket",
            "marble",
            "window",
            "planet",
            "anchor",
            "compass",
            "harbor",
            "feather",
            "meadow",
            "thistle",
            "rainbow",
            "tunnel",
            "pocket",
            "button",
            "helmet",
            "violin",
            "desire",
            "memory",
            "picture",
            "bottle",
            "basket",
            "rocket",
            "island",
            "valley",
            "canyon",
            "thunder",
            "sunrise",
            "moonlight",
            "starlight",
            "snowflake",
            "raindrop",
            "seashell",
            "pineapple",
            "strawberry",
            "blueberry",
            "watermelon",
            "notebook",
            "keyboard",
            "monitor",
            "library",
            "factory",
            "station",
            "airport",
            "village",
            "market",
            "museum",
            "theater",
            "concert",
            "festival",
            "journey",
            "adventure",
            "treasure",
            "diamond",
            "emerald",
            "sapphire",
            "amethyst"
    };

    /**
     * 사용자의 질문이 들어갈 템플릿.
     * %s가 사용자의 질문으로 바뀔 것입니다.
     */
    private static final String USER_QUESTION_TEMPLATE = """
            <!-- START OF USER PROMPT -->
            %s
            <!-- END OF USER PROMPT -->
            """;

    // 의존성 자동 주입.
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final LlmChatHistoryService chatHistoryService;
    private final ChatHistoryNoSqlRepository chatHistoryNoSqlRepository;

    private final SecureRandom rng = new SecureRandom();

    public ChatController(ChatClient.Builder chatClient, VectorStore vectorStore, LlmChatHistoryService chatHistoryService,ChatHistoryNoSqlRepository chatHistoryNoSqlRepository) {
        this.chatClient = chatClient.defaultSystem(SYSTEM_PROMPT).build();
        this.vectorStore = vectorStore;
        this.chatHistoryService = chatHistoryService;
        this.chatHistoryNoSqlRepository = chatHistoryNoSqlRepository;
    }

    // TODO: DB 연동 전 임시 테스트용 ID (실제 개발 시 제거 예정)
    private static final Integer TEMP_COMPANY_ID = 1;
    private static final Integer TEMP_EMPLOYEE_ID = 2;

    /**
     * 사용자에게 질문을 받아 답변을 생성합니다.
     *
     * @param authorization JWT Access Token.
     * @param sessionId     대화 세션 ID.
     * @param userQuestion  사용자의 질문. ({@link UserQuestionDto})
     * @return {@link ChatHistoryNoSql}
     */
    @PostMapping(value = "/chat/sessions/{sessionId}/ask", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatHistoryNoSql> askToLlm(
//            @RequestParam(name = "file", required = false) MultipartFile file,
            @RequestHeader(name = "Authorization") String authorization,
            @PathVariable String sessionId, @RequestBody UserQuestionDto userQuestion
    ) {
        // URL 파라미터의 UUID 파싱 시도
        UUID sessionUuid;
        try {
            sessionUuid = UUID.fromString(sessionId);
        } catch (IllegalArgumentException e) { // 올바르지 않은 UUID일 경우에 Bad Request
            return ResponseEntity.badRequest().build();
        }

        // UUID로 메시지 목록 불러오기
        List<Message> messages = chatHistoryService.getChatHistories().get(sessionUuid);

        // UUID로 목록을 찾을 수 없는 경우 Not Found
        if (messages == null) {
            return ResponseEntity.notFound().build();
        }

        // 사용자의 질문이 비어 있으면 Bad Request
        if (userQuestion.message() == null || userQuestion.message().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // 사용자의 질문을 구분자로 감싸기
        String formattedQuestion = USER_QUESTION_TEMPLATE.formatted(userQuestion.message());

        // 문서 RAG 검색 도구 생성
        RetrievalAugmentationAdvisor ragAdvisor = RetrievalAugmentationAdvisor.builder().documentRetriever(VectorStoreDocumentRetriever.builder().similarityThreshold(0.5).vectorStore(vectorStore).build()).build();
        // LLM 요청 객체 정의
        ChatClient.CallResponseSpec response;

        // LLM에 요청
        response = chatClient.prompt().advisors(ragAdvisor).messages(messages).user(formattedQuestion).call();

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

        // LLM의 답변이 비어 있으면 500 반환
        if (llmAnswer == null || llmAnswer.isBlank()) {
            log.error("LLM이 답변에 실패했습니다. 답변이 비어 있습니다.");
            return ResponseEntity.internalServerError().build();
        }

        // 답변 뒤에 면책 추가
        llmAnswer = llmAnswer.concat("\n\n해당 정보는 정확하지 않을 수 있으며, 사측에서는 해당 답변으로 인해 발생하는 모든 피해에 대해 어떠한 책임도 지지 않습니다.");

        // 대화 내역 기록
        messages.add(UserMessage.builder().text(formattedQuestion).build());
        messages.add(AssistantMessage.builder().content(llmAnswer).build());

        // Ai 답변 결과를 조립
        // TODO: DB 연동 전 임시 테스트용 ID
        ChatHistoryNoSql chatResult = new ChatHistoryNoSql(TEMP_COMPANY_ID,TEMP_EMPLOYEE_ID, sessionId, llmAnswer, LocalDateTime.now(), "assistant");

        // 유저 질문 결과 조립
        ChatHistoryNoSql userHistory = new ChatHistoryNoSql(TEMP_COMPANY_ID, TEMP_EMPLOYEE_ID, sessionId, userQuestion.message(), LocalDateTime.now(), "user");

        // 대화내역 MongoDB저장 (테스트필요)
        try {
            chatHistoryNoSqlRepository.save(userHistory);
            chatHistoryNoSqlRepository.save(chatResult);
        } catch (Exception e) {
            log.error("MongoDB 대화 내역 저장 실패");
        }


        // 성공 시 답변 객체 반환
        return ResponseEntity.ok().body(chatResult);
    }

    /**
     * 새로운 대화방을 만듭니다.
     *
     * @param authorization JWT Access Token.
     * @return {@link ChatSession}
     */
    @PostMapping("/chat/sessionrooms")
    public ResponseEntity<ChatSession> createChatSession(
            @RequestHeader(name = "Authorization") String authorization,
            @RequestBody ChatSessionRequestDto chatSessionRequest
    ) {
        // TODO: JWT 검증 과정 추가
        UUID sessionUuid = UUID.randomUUID();
        // TODO: 회사 정보와 사용자 정보 얻어오기
        Company company = new Company();
        UserMember employee = new UserMember();

        // Title이 있으면 그대로 사용, 아니면 무작위 단어 3개 배합
        String title = chatSessionRequest.title() != null ? chatSessionRequest.title() : "%s-%s-%s".formatted(WORDS[rng.nextInt(0, WORDS.length)], WORDS[rng.nextInt(0, WORDS.length)], WORDS[rng.nextInt(0, WORDS.length)]);

        // 대화 내역 Map에 새로 등록
        chatHistoryService.getChatHistories().put(sessionUuid, new ArrayList<>());

        return ResponseEntity.ok().body(new ChatSession(sessionUuid.toString(), company, employee, title, LocalDateTime.now()));
    }
}
