package com.mes.mesaichatbot.parser;

import com.mes.mesaichatbot.dto.UploadDataDetailsDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class PdfParser implements DocumentParser {
    private final Long companyId;
    private final String fileId;
    private final LocalDateTime createdAt;

    public PdfParser(Long companyId, String fileId, LocalDateTime createdAt) {
        this.companyId = companyId;
        this.fileId = fileId;
        this.createdAt = createdAt;
    }

    @Override
    public boolean supports(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }

        return Objects.equals(StringUtils.getFilenameExtension(fileName), "pdf");
    }

    @Override
    public List<UploadDataDetailsDto> parse(InputStream inputStream) throws IOException {
        // DOCX 문서 열기
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            // 내용을 담을 List 정의
            List<UploadDataDetailsDto> data = new ArrayList<>();

            // PDF 텍스트 추출 도구 생성
            PDFTextStripper stripper = new PDFTextStripper();
            // 각 페이지 순회
            for (int i = 1; i <= document.getNumberOfPages(); i++) {
                // 텍스트를 추출할 페이지 설정
                stripper.setStartPage(i);
                stripper.setEndPage(i);

                // 추출하여 줄 바꿈을 기준으로 분리
                String[] text = stripper.getText(document).split("\\R");
                // 각 항목을 List에 추가
                for (int j = 0; j < text.length; j++) {
                    data.add(new UploadDataDetailsDto(i, companyId, fileId, j + 1, text[j], createdAt));
                }
            }

            // 완성된 데이터 반환
            return data;
        }
    }

}
