package com.mes.mesaichatbot.service;

import com.mes.mesaichatbot.dto.UploadDataDetailsDto;
import com.mes.mesaichatbot.llm.service.LlmChunkingService;
import com.mes.mesaichatbot.parser.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 문서를 파싱합니다.
 */
@Slf4j
@Service
public class DocumentService {
    private final LlmChunkingService llmChunkingService;
    private final VectorStore vectorStore;

    public DocumentService(LlmChunkingService llmChunkingService, VectorStore vectorStore) {
        this.llmChunkingService = llmChunkingService;
        this.vectorStore = vectorStore;
    }

    /**
     * 문서를 파싱하여 UploadDataDetailsDto List를 반환합니다.
     *
     * @param file      {@link MultipartFile} 형식으로 업로드된 파일.
     * @param companyId 기업번호. 아마 토큰으로 얻어와야 할 것입니다.
     * @param fileId    UUID 발급한 것을 여기에 사용하세요.
     * @param createdAt {@code LocalDateTime.now()}로 만든 날짜를 여기에 넣으세요.
     * @return {@code List<UploadDataDetailsDto>}
     * @throws IOException              파일 데이터가 잘못됨.
     * @throws IllegalArgumentException 파일이 잘못된 이름을 가지고 있음.
     */
    public List<UploadDataDetailsDto> parse(MultipartFile file, int companyId, String fileId, LocalDateTime createdAt) throws IOException, IllegalArgumentException {
        // 문서 파서 준비
        List<DocumentParser> parsers = List.of(
                new DocxParser(companyId, fileId, createdAt),
                new XlsxParser(companyId, fileId, createdAt),
                new PptxParser(companyId, fileId, createdAt),
                new CsvParser(companyId, fileId, createdAt),
                new PdfParser(companyId, fileId, createdAt)
        );

        // 파일 이름 가져오기
        String filename = file.getOriginalFilename();
        // 파일 이름 오류 처리
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("파일명이 없습니다.");
        }

        // 확장자에 따라서 적절한 파서 꺼내기
        DocumentParser parser = parsers.stream()
                .filter(p -> p.supports(filename))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "지원하지 않는 파일 형식입니다: " + filename
                        )
                );

        // 문서 처리 시도
        try (InputStream stream = file.getInputStream()) {
            return parser.parse(stream);
        }
    }

    // 문서를 비동기로 MongoDB에 저장.
    @Async("saveToVectorStore")
    public void saveToVectorStore(List<UploadDataDetailsDto> uploadDataDetailsDto, String fileType) {
        CompletableFuture.runAsync(() -> {
                    log.info("Saving to vector store...");
                    List<Document> data;

                    // CSV는 청크하지 않고 그대로 매핑 후 저장
                    if (fileType.equalsIgnoreCase("csv")) {
                        data = new ArrayList<>();
                        for (int i = 0; i < uploadDataDetailsDto.size(); i++) {
                            String content = uploadDataDetailsDto.get(i).rowData();

                            Map<String, Object> metadata =
                                    new HashMap<>();


                            metadata.put(
                                    "companyId",
                                    uploadDataDetailsDto.get(i).companyId()
                            );

                            metadata.put(
                                    "fileId",
                                    uploadDataDetailsDto.get(i).fileId()
                            );

                            metadata.put(
                                    "rowNumbers",
                                    uploadDataDetailsDto.get(i).rowNumber()
                            );

                            metadata.put(
                                    "startRowNumber",
                                    0
                            );

                            metadata.put(
                                    "endRowNumber",
                                    uploadDataDetailsDto.size()
                            );

                            data.add(new Document(
                                    String.format("doc-%s-%s", uploadDataDetailsDto.get(i).fileId(), "chunk-%03d".formatted(i + 1)),
                                    content,
                                    metadata
                            ));
                        }
                    } else {
                        // 청크 시도
                        data = llmChunkingService.chunk(uploadDataDetailsDto);
                    }
                    // 저장 시도
                    vectorStore.add(data);
                }).exceptionally(e -> {
                    // 예외 발생 시 기록 후 중지
                    log.error(String.valueOf(e));
                    return null;
                })
                // 문제 없으면 Log 출력
                .thenRun(() -> log.info("Save to vector store complete."));
    }
}
