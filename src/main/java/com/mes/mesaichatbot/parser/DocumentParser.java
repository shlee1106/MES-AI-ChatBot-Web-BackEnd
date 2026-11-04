package com.mes.mesaichatbot.parser;

import com.mes.mesaichatbot.dto.UploadDataDetailsDto;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 문서 파서입니다.
 * PPTX, DOCX, XLSX일 수 있습니다.
 */
public interface DocumentParser {
    /**
     * 해당 Parser가 파일명을 지원하는지 확인합니다.
     *
     * @param fileName 파일의 이름.
     * @return true or false.
     */
    boolean supports(String fileName);

    /**
     * 문서를 파싱하여 {@code List<UploadDataDetailsDto>} 형식으로 반환합니다.
     *
     * @param stream 파일의 {@link InputStream}.
     * @return {@code List<UploadDataDetailsDto>}
     * @throws IOException 파일이 잘못됨.
     */
    List<UploadDataDetailsDto> parse(InputStream stream) throws IOException;
}
