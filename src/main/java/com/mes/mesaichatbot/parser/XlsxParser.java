package com.mes.mesaichatbot.parser;

import com.mes.mesaichatbot.dto.UploadDataDetailsDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class XlsxParser implements DocumentParser {
    private final int companyId;
    private final String fileId;
    private final LocalDateTime createdAt;

    public XlsxParser(int companyId, String fileId, LocalDateTime createdAt) {
        this.companyId = companyId;
        this.fileId = fileId;
        this.createdAt = createdAt;
    }

    @Override
    public boolean supports(String filename) {
        if (filename == null || filename.isBlank()) {
            return false;
        }

        return Objects.equals(StringUtils.getFilenameExtension(filename), "xlsx");
    }

    @Override
    public List<UploadDataDetailsDto> parse(InputStream inputStream) throws IOException {
        List<UploadDataDetailsDto> data = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                for (int j = 0; j < sheet.getLastRowNum(); j++) {
                    Row row = sheet.getRow(j);
                    
                    data.add(new UploadDataDetailsDto(i + 1, companyId, fileId, j, objectMapper.writeValueAsString(row), createdAt));
                }
            }

        }

        return data;
    }
}