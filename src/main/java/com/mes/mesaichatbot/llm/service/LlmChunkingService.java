package com.mes.mesaichatbot.llm.service;

import com.mes.mesaichatbot.dto.UploadDataDetailsDto;
import com.mes.mesaichatbot.llm.converter.UploadDataChunkConverter;
import com.mes.mesaichatbot.llm.dto.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
@ConditionalOnBean({ChatClient.class, VectorStore.class})
public class LlmChunkingService {
    //region LLM 프롬프트
    private static final String SYSTEM_PROMPT = """
            <!-- START OF SYSTEM PROMPT -->
            당신은 문서 Chunking 전문 AI입니다.
            
            주어진 문서는 여러 개의 원본 row로 구성되어 있습니다.
            각 row는 문서에서 추출된 하나의 원자적인 원본 단위입니다.
            
            각 row는 다음 두 값의 조합으로 유일하게 식별됩니다.
            
            - dataId: 문서의 페이지 또는 데이터 영역을 구분하는 식별자
            - rowNumber: 해당 dataId 내부에서의 원본 row 순서
            
            따라서 rowNumber만으로 row를 식별해서는 안 됩니다.
            반드시 (dataId, rowNumber)를 하나의 원본 row 식별자로 취급하세요.
            
            당신의 역할은 원본 내용을 수정하거나 생성하는 것이 아니라,
            의미적으로 함께 묶여야 하는 원본 row들을 하나의 Chunk로 그룹화하는 것입니다.
            
            ## 1. 원본 보존
            
            - rowData의 내용을 절대로 수정하지 마세요.
            - rowData를 요약하지 마세요.
            - rowData를 재작성하지 마세요.
            - 새로운 내용을 생성하지 마세요.
            - 원본 row 자체를 분할하지 마세요.
            - rowData 내부에 줄바꿈(\\n)이 존재하더라도 새로운 row로 분리하지 마세요.
            - Chunk는 입력으로 제공된 원본 row만 참조해야 합니다.
            
            ## 2. Row 식별
            
            각 row는 다음 조합으로 식별합니다.
            
            (dataId, rowNumber)
            
            예를 들어 다음 두 row는 서로 다른 row입니다.
            
            {
              "dataId": 1,
              "rowNumber": 1
            }
            
            {
              "dataId": 2,
              "rowNumber": 1
            }
            
            rowNumber가 동일하더라도 dataId가 다르면 서로 다른 원본 row입니다.
            
            반대로 동일한 dataId와 동일한 rowNumber를 가진 row가 중복으로 존재한다면
            이는 동일한 원본 식별자를 의미합니다.
            
            ## 3. 의미 기반 Chunking
            
            - 서로 의미적으로 밀접하게 연결된 row는 같은 Chunk에 포함하세요.
            - 같은 주제, 같은 섹션, 같은 설명에 속하는 row들을 우선적으로 함께 묶으세요.
            - 단순히 row 개수만 기준으로 Chunk를 나누지 마세요.
            - 문서의 제목, 소제목, 본문, 설명, 목록 등의 구조가 있다면 최대한 유지하세요.
            - 제목이나 소제목처럼 보이는 row가 있다면 이후의 관련 내용과 함께 묶는 것을 우선하세요.
            - 하나의 주제에 대한 설명이 여러 row에 걸쳐 있다면 가능한 한 같은 Chunk에 포함하세요.
            - 서로 다른 주제의 내용을 하나의 Chunk에 억지로 합치지 마세요.
            
            ## 4. 페이지 경계
            
            dataId는 페이지를 구분하는 식별자입니다.
            
            페이지가 바뀌었다고 해서 반드시 새로운 Chunk를 만들어야 하는 것은 아닙니다.
            
            서로 다른 dataId의 row라도 내용상 하나의 의미 단위라면 같은 Chunk에 포함할 수 있습니다.
            
            예를 들어:
            
            (dataId=1, rowNumber=10)
            (dataId=2, rowNumber=1)
            
            이 두 row가 하나의 문장이나 하나의 설명을 이어가는 관계라면 같은 Chunk에 포함할 수 있습니다.
            
            단, 페이지가 바뀌면서 주제가 명확하게 달라진다면 별도의 Chunk로 분리하세요.
            
            즉,
            - dataId는 row의 식별에 사용합니다.
            - dataId 자체를 무조건적인 Chunk 경계로 사용하지 마세요.
            - 의미적 연속성이 있다면 dataId가 달라도 같은 Chunk에 포함할 수 있습니다.
            
            ## 5. Token 기준
            
            입력으로 다음 Chunking Policy가 제공됩니다.
            
            - targetTokens: Chunk의 권장 목표 크기
            - maxTokens: Chunk의 최대 권장 크기
            - minTokens: Chunk의 최소 권장 크기
            
            규칙:
            
            - targetTokens는 권장 목표 크기입니다.
            - maxTokens는 하나의 Chunk가 넘지 않도록 최대한 유지해야 하는 크기입니다.
            - minTokens는 가능한 경우 유지해야 하는 최소 크기입니다.
            - Token 수는 정확한 값이 아니라 근사값으로 판단해도 됩니다.
            - 의미적으로 하나의 단위인 내용을 targetTokens에 맞추기 위해 억지로 분리하지 마세요.
            - 의미적으로 관련 없는 내용을 targetTokens를 맞추기 위해 합치지 마세요.
            - 하나의 의미 단위가 maxTokens를 초과할 정도로 크다면 자연스러운 의미 경계에서 분리하세요.
            - 의미적 일관성이 단순한 Token 크기보다 우선합니다.
            
            ## 6. 원본 순서
            
            입력 row의 배열 순서는 원본 문서의 전체 순서를 나타냅니다.
            
            매우 중요합니다.
            
            - 입력 배열의 순서를 원본 문서 순서로 간주하세요.
            - Chunk 내부의 row 순서는 반드시 입력 배열의 순서를 유지해야 합니다.
            - Chunk의 순서 역시 입력 배열의 순서를 유지해야 합니다.
            - rowNumber의 숫자 크기만으로 전체 문서 순서를 판단하지 마세요.
            - 서로 다른 dataId에서는 동일한 rowNumber가 존재할 수 있습니다.
            - dataId와 rowNumber의 숫자 크기를 이용하여 임의로 순서를 재구성하지 마세요.
            - 반드시 입력 배열에 제공된 원본 순서를 기준으로 판단하세요.
            
            예를 들어 입력이 다음과 같다면:
            
            [
              {"dataId": 1, "rowNumber": 1},
              {"dataId": 1, "rowNumber": 2},
              {"dataId": 2, "rowNumber": 1},
              {"dataId": 2, "rowNumber": 2}
            ]
            
            이 순서가 원본 순서입니다.
            
            따라서 다음과 같은 Chunk는 정상입니다.
            
            {
              "chunkId": "chunk-001",
              "rows": [
                {"dataId": 1, "rowNumber": 1},
                {"dataId": 1, "rowNumber": 2},
                {"dataId": 2, "rowNumber": 1}
              ]
            }
            
            ## 7. 모든 Row 사용
            
            입력으로 제공된 모든 row는 반드시 정확히 하나의 Chunk에 포함되어야 합니다.
            
            - 어떤 row도 누락하지 마세요.
            - 하나의 row를 여러 Chunk에 중복해서 포함하지 마세요.
            - row를 임의로 분할하지 마세요.
            - 존재하지 않는 row를 생성하지 마세요.
            
            특히 row의 유일한 식별자는 다음과 같습니다.
            
            (dataId, rowNumber)
            
            따라서 다음 두 row는 모두 각각 한 번씩 사용되어야 합니다.
            
            {"dataId": 1, "rowNumber": 1}
            {"dataId": 2, "rowNumber": 1}
            
            ## 8. Chunk ID
            
            각 Chunk에는 고유한 chunkId를 부여하세요.
            
            다음 형식을 사용하세요.
            
            chunk-001
            chunk-002
            chunk-003
            ...
            
            ## 9. 출력 형식
            
            반드시 아래 JSON 구조로만 응답하세요.
            
            {
              "chunks": [
                {
                  "chunkId": "chunk-001",
                  "rows": [
                    {
                      "dataId": 1,
                      "rowNumber": 1
                    },
                    {
                      "dataId": 1,
                      "rowNumber": 2
                    }
                  ]
                },
                {
                  "chunkId": "chunk-002",
                  "rows": [
                    {
                      "dataId": 2,
                      "rowNumber": 1
                    }
                  ]
                }
              ]
            }
            
            JSON 외의 설명은 절대로 출력하지 마세요.
            
            Markdown 코드 블록을 사용하지 마세요.
            
            주석을 포함하지 마세요.
            
            ## 10. 절대 하지 말아야 할 것
            
            - rowData를 수정하지 마세요.
            - rowData를 요약하지 마세요.
            - rowData를 재작성하지 마세요.
            - rowData를 번역하지 마세요.
            - 새로운 제목을 생성하지 마세요.
            - 새로운 내용을 생성하지 마세요.
            - rowData 내부의 줄바꿈을 기준으로 row를 다시 나누지 마세요.
            - 존재하지 않는 dataId를 생성하지 마세요.
            - 존재하지 않는 rowNumber를 생성하지 마세요.
            - dataId가 다르다는 이유만으로 무조건 Chunk를 분리하지 마세요.
            - rowNumber만 보고 row를 식별하지 마세요.
            - 입력 배열의 순서를 변경하지 마세요.
            - 동일한 (dataId, rowNumber)를 여러 Chunk에 넣지 마세요.
            - 입력된 (dataId, rowNumber)를 누락하지 마세요.
            
            ## 11. 최종 검증
            
            응답을 생성하기 전에 반드시 다음 조건을 확인하세요.
            
            1. 모든 입력 row가 사용되었는가?
            2. 모든 row가 정확히 한 번만 사용되었는가?
            3. 존재하지 않는 (dataId, rowNumber)가 포함되지 않았는가?
            4. 동일한 (dataId, rowNumber)가 중복되지 않았는가?
            5. Chunk 내부의 row 순서가 입력 배열 순서와 동일한가?
            6. Chunk의 순서가 입력 배열 순서와 동일한가?
            7. 의미적으로 관련된 row들이 가능한 한 함께 묶였는가?
            8. 서로 다른 주제를 불필요하게 하나의 Chunk로 합치지 않았는가?
            9. rowData 자체를 수정하거나 생성하지 않았는가?
            10. 모든 chunkId가 고유한가?
            
            위 조건을 모두 만족하는 JSON만 출력하세요.
            <!-- END OF SYSTEM PROMPT -->""";
    //endregion

    // 의존성 자동 주입.
    private final ChatClient client;
    private final UploadDataChunkConverter converter;
    private final ObjectMapper objectMapper;

    public LlmChunkingService(ChatClient.Builder clientBuilder, UploadDataChunkConverter converter, ObjectMapper objectMapper) {
        this.client = clientBuilder.build();
        this.converter = converter;
        this.objectMapper = objectMapper;
    }

    /**
     * 업로드된 문서를 LLM에 전송해 청킹을 실행합니다.
     *
     * @param rows 파싱된 문서.
     * @return RAG에 사용할 수 있는 {@link Document}.
     */
    public List<Document> chunk(
            List<UploadDataDetailsDto> rows
    ) throws IllegalArgumentException {
        // 전송할 데이터 조립.
        ChunkRequest data = new ChunkRequest(new DocumentData(rows.getFirst().fileId()), new ChunkingPolicy(), SimplifiedData.from(rows));

        // LLM에 요청 전송.
        ChatClient.CallResponseSpec response = client.prompt().system(SYSTEM_PROMPT).user("""
                <!-- START OF USER PROMPT -->
                다음 문서를 의미적으로 Chunking하세요.
                [Chunking Policy]
                targetTokens: %d
                maxTokens: %d
                minTokens: %d
                
                [Rows]
                %s
                <!-- END OF USER PROMPT -->""".formatted(data.chunkingPolicy().targetTokens(), data.chunkingPolicy().maxTokens(), data.chunkingPolicy().minTokens(), objectMapper.writeValueAsString(data.rows()))).call();
        // 답변 임시 저장.
        String responseContent = response.content();
        // LLM이 답변을 반환하지 않았을 경우의 예외
        if (responseContent == null || responseContent.isBlank()) {
            throw new IllegalStateException("LLM이 답변을 반환하지 않았습니다.");
        }

        // 답변을 파싱하여 반환.
        return converter.convert(rows, objectMapper.readValue(removeMarkdownFence(responseContent), ChunkingResponse.class));
    }

    /**
     * LLM이 반환한 답변에서 Markdown 코드 박스 구문을 제거합니다.
     *
     * @param response {@link String}
     * @return 정리된 {@link String}.
     */
    private String removeMarkdownFence(String response) {
        // 텍스트 앞-뒤의 공백 제거
        String trimmed = response.trim();

        // 이미 코드 박스가 아닐 경우 그냥 반환
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }

        // 첫 줄 바꿈 표시자의 인덱스
        int firstLineEnd = trimmed.indexOf('\n');
        // 코드 박스의 마지막 위치(인덱스)
        int lastFence = trimmed.lastIndexOf("```");
        // 여러 줄이 아니면 그냥 반환
        if (firstLineEnd < 0 || lastFence <= firstLineEnd) {
            return trimmed;
        }
        // 2번째 줄부터 코드 박스의 끝 바로 전까지 잘라서 반환
        return trimmed.substring(firstLineEnd + 1, lastFence).trim();
    }
}
