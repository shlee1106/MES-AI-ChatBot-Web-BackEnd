package com.mes.mesaichatbot.parser;

import com.mes.mesaichatbot.dto.UploadDataDetailsDto;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CsvParser implements DocumentParser {
    //region 필요한 데이터 복사
    private final Long companyId;
    private final String fileId;
    private final LocalDateTime createdAt;

    public CsvParser(Long companyId, String fileId, LocalDateTime createdAt) {
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

        return Objects.equals(StringUtils.getFilenameExtension(filename), "csv");
    }

    public List<UploadDataDetailsDto> parse(InputStream inputStream) throws IOException {
        // JSON Mapper 생성
        ObjectMapper mapper = new ObjectMapper();

        // 파싱 결과를 담을 List 생성
        List<UploadDataDetailsDto> data = new ArrayList<>();

        // CSV 데이터를 InputStream에서 읽기
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            List<String[]> rows = reader.readAll();
            if (rows.isEmpty()) {
                throw new IOException("CSV 파일이 비어 있습니다.");
            }
            String[] head = rows.getFirst();

            // 각 행 순회
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                List<String> everyRows = new ArrayList<>();
                // 각 열 매핑 (헤더보다 열이 적은 행은 모자란 칸을 빈 문자열로 처리)
                for (int j = 0; j < head.length; j++) {
                    everyRows.add("%s: %s\n".formatted(head[j], j < row.length ? row[j] : ""));
                }
                // 데이터 추가
                data.add(new UploadDataDetailsDto(1, companyId, fileId, i + 1, mapper.writeValueAsString(everyRows), createdAt));
            }
        } catch (CsvException e) {
            // CsvException 발생 시 사용자가 잘못된 데이터를 던진 것으로 간주.
            throw new IOException(e);
        }

        return data;
    }
}
