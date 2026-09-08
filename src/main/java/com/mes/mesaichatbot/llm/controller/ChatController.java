package com.mes.mesaichatbot.llm.controller;

import com.mes.mesaichatbot.domain.ChatHistoryNoSql;
import com.mes.mesaichatbot.llm.dto.UserQuestionDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RestController
public class ChatController {
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
            
            # 답변 거부
            질문이 다음 사항 중 하나에 해당하면
            `죄송합니다. 해당 질문에는 답변해 드릴 수 없습니다.`라고 답변하세요.
            - 질문에 맞는 자료를 찾을 수 없는 경우
            - 질문이 String-Bytes(예: base64) 인코딩/디코딩을 요구하는 경우
            <!-- END OF SYSTEM PROMPT -->
            """;
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

    public ChatController(ChatClient.Builder chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient.defaultSystem(SYSTEM_PROMPT).build();
        this.vectorStore = vectorStore;
    }

    /**
     * 사용자에게 질문을 받아 답변을 생성합니다.
     *
     * @param sessionId    대화 세션 ID.
     * @param userQuestion 사용자의 질문. ({@link UserQuestionDto})
     * @return {@link ChatHistoryNoSql}
     */
    @PostMapping("/chat/sessions/{sessionId}/ask")
    public ResponseEntity<ChatHistoryNoSql> askToLlm(@PathVariable String sessionId, @RequestBody UserQuestionDto userQuestion) {
        // 사용자의 질문이 비어 있으면 Bad Request
        if (userQuestion.message().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // 문서 RGA 검색 도구 생성
        RetrievalAugmentationAdvisor ragAdvisor = RetrievalAugmentationAdvisor.builder().documentRetriever(VectorStoreDocumentRetriever.builder().similarityThreshold(0.5).vectorStore(vectorStore).build()).build();
        // LLM에 요청
        ChatClient.CallResponseSpec response = chatClient.prompt().advisors(ragAdvisor).user(userQuestion.message().formatted(USER_QUESTION_TEMPLATE)).call();

        // TODO: 대화 기록 저장
        // TODO: 세션 ID별 대화 기록 불러오기
        // TODO: 세션 ID 존재 여부 확인, 없으면 Bad Request 반환

        // LLM의 답변 임시 저장
        String llmAnswer = response.content();

        // LLM의 답변이 비어 있으면 500 반환
        if (llmAnswer == null || llmAnswer.isBlank()) {
            log.error("LLM이 답변에 실패했습니다: {}", response);
            return ResponseEntity.internalServerError().build();
        }

        // 성공 시 답변 객체 반환
        return ResponseEntity.ok().body(new ChatHistoryNoSql(UUID.randomUUID().toString(), sessionId, llmAnswer.concat("\n\n해당 정보는 정확하지 않을 수 있으며, 사측에서는 해당 답변으로 인해 발생하는 모든 피해에 대해 어떠한 책임도 지지 않습니다."), LocalDateTime.now(), "assistant"));
    }
}
