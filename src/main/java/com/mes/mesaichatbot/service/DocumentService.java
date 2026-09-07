package com.mes.mesaichatbot.service;

import com.mes.mesaichatbot.dto.UploadDataDetailsDto;
import com.mes.mesaichatbot.parser.DocumentParser;
import com.mes.mesaichatbot.parser.DocxParser;
import com.mes.mesaichatbot.parser.PptxParser;
import com.mes.mesaichatbot.parser.XlsxParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 문서를 파싱합니다.
 */
@Slf4j
@Service
public class DocumentService {
    /**
     * 문서를 파싱하여 UploadDataDetailsDto List를 반환합니다.
     *
     * @param file      {@link MultipartFile} 형식으로 업로드된 파일.
     * @param companyId 기업번호. 아마 토큰으로 얻어와야 할 것입니다.
     * @param fileId    UUID 발급한 것을 여기에 사용하세요.
     * @param createdAt <code>LocalDateTime.now()</code>로 만든 날짜를 여기에 넣으세요.
     * @return {@code List<UploadDataDetailsDto>}
     * @throws IOException <code>file</code>이 잘못됨. 사용자를 탓해봐요.
     */
    public List<UploadDataDetailsDto> parse(MultipartFile file, int companyId, String fileId, LocalDateTime createdAt) throws IOException, IllegalArgumentException {
        List<DocumentParser> parsers = List.of(
                new DocxParser(companyId, fileId, createdAt),
                new XlsxParser(companyId, fileId, createdAt),
                new PptxParser(companyId, fileId, createdAt)
        );
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("파일명이 없습니다.");
        }

        DocumentParser parser = parsers.stream()
                .filter(p -> p.supports(filename))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "지원하지 않는 파일 형식입니다: " + filename
                        )
                );

        try (InputStream stream = file.getInputStream()) {
            return parser.parse(stream);
        }
    }
}
