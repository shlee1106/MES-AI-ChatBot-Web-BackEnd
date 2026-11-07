package com.mes.mesaichatbot.dto;

import com.mes.mesaichatbot.domain.FileMetadata;

import java.time.LocalDateTime;
import java.util.List;

public record FileResponseDto(String fileId, Long companyId,
                              Long employeeId,
                              String originName, String fileType,
                              boolean shared,
                              LocalDateTime uploadedAt) {
    public FileResponseDto(String fileId, Long companyId, Long employeeId,
                           String originName, String fileType, LocalDateTime uploadedAt) {
        this(fileId, companyId, employeeId, originName, fileType, false, uploadedAt);
    }

    public static FileResponseDto from(FileMetadata metadata) {
        return new FileResponseDto(metadata.getFileId(), metadata.getCompanyId().getCompanyId(),
                                   metadata.getEmployeeId().getEmployeeId(), metadata.getOriginName(),
                                   metadata.getFileType(), metadata.isShared(), metadata.getUploadedAt());
    }

    public static List<FileResponseDto> from(List<FileMetadata> metadataList) {
        return metadataList.parallelStream().map(FileResponseDto::from).toList();
    }
}
