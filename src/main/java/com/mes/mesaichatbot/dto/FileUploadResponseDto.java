package com.mes.mesaichatbot.dto;

import com.mes.mesaichatbot.domain.OriginalFile;

import java.util.List;

public record FileUploadResponseDto(OriginalFile fileData, List<UploadDataDetailsDto> parsedData) {
}
