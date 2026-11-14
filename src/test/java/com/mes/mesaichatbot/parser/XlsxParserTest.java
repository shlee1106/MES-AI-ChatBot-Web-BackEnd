package com.mes.mesaichatbot.parser;

import com.mes.mesaichatbot.dto.UploadDataDetailsDto;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XlsxParserTest {

    /**
     * 워크북을 바이트 배열로 변환합니다.
     *
     * @param workbook 변환할 워크북.
     * @return 워크북의 바이트 배열.
     * @throws IOException 쓰기 실패 시 발생합니다.
     */
    private byte[] toBytes(XSSFWorkbook workbook) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

    @Test
    @DisplayName("중간에 빈 행이 있어도 뒤쪽 행이 모두 파싱되는가?")
    void parsesRowsAfterEmptyRows() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("시트1");
            sheet.createRow(0).createCell(0).setCellValue("first");
            sheet.createRow(2).createCell(0).setCellValue("third");
            sheet.createRow(5).createCell(0).setCellValue("sixth");

            LocalDateTime now = LocalDateTime.now();
            List<UploadDataDetailsDto> result = new XlsxParser(1L, "file-1", now)
                    .parse(new ByteArrayInputStream(toBytes(workbook)));

            assertEquals(3, result.size());
            assertEquals(1, result.get(0).rowNumber());
            assertEquals(3, result.get(1).rowNumber());
            assertEquals(6, result.get(2).rowNumber());
            assertTrue(result.get(0).rowData().contains("first"));
            assertTrue(result.get(1).rowData().contains("third"));
            assertTrue(result.get(2).rowData().contains("sixth"));
        }
    }

    @Test
    @DisplayName("시트가 2개면 시트 번호가 dataId에 반영되는가?")
    void assignsSheetNumberAsDataIdForMultipleSheets() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            workbook.createSheet("시트1").createRow(0).createCell(0).setCellValue("A");
            workbook.createSheet("시트2").createRow(0).createCell(0).setCellValue("B");

            List<UploadDataDetailsDto> result = new XlsxParser(1L, "file-1", LocalDateTime.now())
                    .parse(new ByteArrayInputStream(toBytes(workbook)));

            assertEquals(2, result.size());
            assertEquals(1, result.get(0).dataId());
            assertEquals(2, result.get(1).dataId());
        }
    }
}