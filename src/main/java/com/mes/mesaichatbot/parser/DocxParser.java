package com.mes.mesaichatbot.parser;

import com.mes.mesaichatbot.dto.UploadDataDetailsDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
public class DocxParser implements DocumentParser {
    private final int companyId;
    private final String fileId;
    private final LocalDateTime createdAt;

    public DocxParser(int companyId, String fileId, LocalDateTime createdAt) {
        this.companyId = companyId;
        this.fileId = fileId;
        this.createdAt = createdAt;
    }

    @Override
    public boolean supports(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }

        return Objects.equals(StringUtils.getFilenameExtension(fileName), "docx");
    }

    @Override
    public List<UploadDataDetailsDto> parse(InputStream inputStream) throws IOException {
        // DOCX 문서 열기
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            // 내용을 담을 List 정의
            List<UploadDataDetailsDto> data = new ArrayList<>();
            // JSON 오브젝트 Mapper 정의
            ObjectMapper mapper = new ObjectMapper();

            // DOCX 문서의 내용 꺼내기
            List<IBodyElement> element = document.getBodyElements();

            int rowNumber = 1;
            // 그리고 순회
            for (IBodyElement iBodyElement : element) {
                if (iBodyElement instanceof XWPFParagraph paragraph) {
                    // 텍스트 데이터 처리
                    String text = paragraph.getText();

                    // 빈 텍스트 필터링
                    if (!text.isBlank()) {
                        data.add(new UploadDataDetailsDto(1, companyId, fileId, rowNumber++, text, createdAt));
                    }
                } else if (iBodyElement instanceof XWPFTable table) {
                    // 표 데이터 처리를 위한 2차원 리스트 생성
                    List<List<String>> tableList = new ArrayList<>();
                    // 표 행 순회
                    for (XWPFTableRow row : table.getRows()) {
                        List<String> rowMap = new ArrayList<>();
                        // 표 열 순회
                        for (XWPFTableCell cell : row.getTableCells()) {
                            // 각 칸을 행에 추가
                            rowMap.add(cell.getText());
                        }
                        // 각 행을 추가
                        tableList.add(rowMap);
                    }

                    // 최종 파싱된 표 리스트에 추가
                    data.add(new UploadDataDetailsDto(1, companyId, fileId, rowNumber++, mapper.writeValueAsString(tableList), createdAt));
                }
                // OCR: 비용 문제가 우려되어 제거함.
//                    case XWPFPictureData picture -> {
//                        LlmOcrService ocrService = new LlmOcrService();
//                        data.add(new UploadDataDetailsDto(1, companyId, fileId, i + 1, ocrService.recognizeImage(picture), createdAt));
//                    }
            }

            return data;
        }
    }

}
