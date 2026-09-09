package com.mes.mesaichatbot.dto;

import java.util.List;

public record FileUploadResponseDto(OriginalFileDto fileData, List<UploadDataDetailsDto> parsedData) {
}
