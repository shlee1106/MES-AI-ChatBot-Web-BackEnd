package com.mes.mesaichatbot.dto;

import com.mes.mesaichatbot.domain.FileMetadata;

import java.time.LocalDateTime;

/**
 * 파일 메타데이터 DTO
 * @param fileId 파일 ID
 * @param companyId 회사번호
 * @param employeeId 사원번호
 * @param originName 파일 이름
 * @param fileType 파일 유형
 * @param shared 공유 여부
 * @param uploadedAt 업로드 일시
 */
public record FileMetadataDto(
        String fileId,
        Long companyId,
        Long employeeId,
        String originName,
        String fileType,
        boolean shared,
        LocalDateTime uploadedAt
) {
    /**
     * 파일 메타데이터를 DTO로 변환합니다.
     * @param metadata 파일 메타데이터
     * @return DTO
     */
    public static FileMetadataDto of(FileMetadata metadata) {
        return new FileMetadataDto(metadata.getFileId(), metadata.getCompanyId().getCompanyId(), metadata.getEmployeeId().getEmployeeId(), metadata.getOriginName(), metadata.getFileType(), metadata.isShared(), metadata.getUploadedAt());
    }
}
