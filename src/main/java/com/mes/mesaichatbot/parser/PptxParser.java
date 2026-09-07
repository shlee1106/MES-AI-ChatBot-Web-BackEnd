package com.mes.mesaichatbot.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.mesaichatbot.dto.UploadDataDetailsDto;
import org.apache.poi.xslf.usermodel.*;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * PPTX 파일을 전담하여 파싱하는 클래스
 */
public class PptxParser implements DocumentParser {

    //region 필요한 데이터 복사
    private final int companyId;
    private final String fileId;
    private final LocalDateTime createdAt;

    public PptxParser(int companyId, String fileId, LocalDateTime createdAt) {
        this.companyId = companyId;
        this.fileId = fileId;
        this.createdAt = createdAt;
    }
    //endregion

    @Override
    public boolean supports(String filename) {
        if (filename == null || filename.isBlank()) {
            return false;
        }

        return Objects.equals(StringUtils.getFilenameExtension(filename), "pptx");
    }

    @Override
    public List<UploadDataDetailsDto> parse(InputStream inputStream) throws IOException {
        // 파싱 결과를 담을 List 생성
        List<UploadDataDetailsDto> data = new ArrayList<>();
        // JSON Mapper 생성
        ObjectMapper mapper = new ObjectMapper();

        // inputStream에서 PPTX 읽기
        try (XMLSlideShow presentation = new XMLSlideShow(inputStream)) {
            // 매 슬라이드 순회
            for (int i = 0; i < presentation.getSlides().size(); i++) {
                XSLFSlide slide = presentation.getSlides().get(i);
                int rowNum = 1;

                // 각 슬라이드에 있는 객체 순회
                for (XSLFShape shape : slide.getShapes()) {
                    // 텍스트만 추출.
                    // 다른 객체는 너무 복잡해서 생략함.
                    if (shape instanceof XSLFTextShape textShape) {
                        String text = textShape.getText();
                        // 빈 텍스트 필터링
                        if (text.isBlank()) {
                            continue;
                        }

                        // 각 텍스트를 List에 추가
                        data.add(new UploadDataDetailsDto(i + 1, companyId, fileId, rowNum++, text, createdAt));
                    } else if (shape instanceof XSLFTable table) {
                        // 표 데이터 처리를 위한 2차원 리스트 생성
                        List<List<String>> tableList = new ArrayList<>();
                        // 표 행 순회
                        for (XSLFTableRow row : table.getRows()) {
                            List<String> rowMap = new ArrayList<>();
                            // 표 열 순회
                            for (XSLFTableCell cell : row.getCells()) {
                                // 각 칸을 행에 추가
                                rowMap.add(cell.getText());
                            }
                            // 각 행을 추가
                            tableList.add(rowMap);
                        }

                        // 최종 파싱된 표 리스트에 추가
                        data.add(new UploadDataDetailsDto(1, companyId, fileId, i + 1, mapper.writeValueAsString(tableList), createdAt));
                    }
                }
            }
        }
        // 완성된 List 반환
        return data;
    }
}
