package com.mes.mesaichatbot.dto;

import com.mes.mesaichatbot.domain.FileMetadata;

import java.util.List;

public record FileUploadResponseDto(String message, FileMetadataDto fileData, List<UploadDataDetailsDto> parsedData) {
    /**
     * 파일 업로드가 성공하였을 때의 DTO를 생성합니다.
     *
     * @param fileData   파일 메타데이터.
     * @param parsedData 처리된 데이터.
     * @return 성공 DTO.
     */
    public static FileUploadResponseDto ok(FileMetadata fileData, List<UploadDataDetailsDto> parsedData) {
        return new FileUploadResponseDto("파일이 성공적으로 업로드되었습니다.", FileMetadataDto.of(fileData), parsedData);
    }
}
