package com.mes.mesaichatbot.dto;

import com.mes.mesaichatbot.domain.FileMetadata;

import java.util.List;

public record FileUploadResponseDto(FileMetadata fileData, List<UploadDataDetailsDto> parsedData) {
}
