package com.mes.mesaichatbot.parser;

import com.mes.mesaichatbot.dto.UploadDataDetailsDto;
import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@AllArgsConstructor
public class XlsxParser implements DocumentParser {
    private final Long companyId;
    private final String fileId;
    private final LocalDateTime createdAt;

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
        DataFormatter dataFormatter = new DataFormatter();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                for (int j = 0; j < sheet.getPhysicalNumberOfRows(); j++) {
                    if (sheet.getRow(j) == null) {
                        continue;
                    }

                    List<String> cells = new ArrayList<>();
                    for (Cell cell : sheet.getRow(j)) {
                        cells.add(dataFormatter.formatCellValue(cell));
                    }

                    data.add(new UploadDataDetailsDto(i + 1, companyId, fileId, j + 1, objectMapper.writeValueAsString(cells), createdAt));
                }
            }

        }

        return data;
    }
}