package com.mes.mesaichatbot.parser;

import com.mes.mesaichatbot.dto.UploadDataDetailsDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            List<UploadDataDetailsDto> data = new ArrayList<>();

            List<IBodyElement> element = document.getBodyElements();
            for (int i = 0; i < element.size(); i++) {
                if (element.get(i) instanceof XWPFParagraph paragraph) {
                    String text = paragraph.getText();

                    if (!text.isBlank()) {
                        data.add(new UploadDataDetailsDto(1, companyId, fileId, i + 1, text, createdAt));
                    }
                } else if (element.get(i) instanceof XWPFTable table) {
                    ObjectMapper mapper = new ObjectMapper();
                    data.add(new UploadDataDetailsDto(1, companyId, fileId, i + 1, mapper.writeValueAsString(table), createdAt));
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
