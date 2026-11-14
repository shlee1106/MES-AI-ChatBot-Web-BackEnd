package com.mes.mesaichatbot.parser;

import com.mes.mesaichatbot.dto.UploadDataDetailsDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvParserTest {

    @Test
    @DisplayName("빈 CSV 파일이면 IOException이 발생하는가?")
    void throwsIOExceptionForEmptyFile() {
        CsvParser parser = new CsvParser(1L, "file-1", LocalDateTime.now());

        assertThrows(IOException.class,
                     () -> parser.parse(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    @DisplayName("헤더보다 열이 적은 행이 있어도 예외 없이 빈 칸으로 처리되는가?")
    void treatsMissingColumnsAsEmptyString() throws IOException {
        CsvParser parser = new CsvParser(1L, "file-1", LocalDateTime.now());
        String csv = "a,b,c\n1,2\n";

        List<UploadDataDetailsDto> result = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertEquals(1, result.size());
        assertEquals(2, result.getFirst().rowNumber());
        assertTrue(result.getFirst().rowData().contains("c: "));
    }

    @Test
    @DisplayName("정상 CSV이면 헤더를 제외한 데이터 행이 모두 파싱되는가?")
    void parsesNormalCsv() throws IOException {
        CsvParser parser = new CsvParser(1L, "file-1", LocalDateTime.now());
        String csv = "a,b\n1,2\n3,4\n";

        List<UploadDataDetailsDto> result = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertEquals(2, result.size());
    }
}