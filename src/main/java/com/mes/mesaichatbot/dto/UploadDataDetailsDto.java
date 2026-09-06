package com.mes.mesaichatbot.dto;

import java.time.LocalDateTime;

public record UploadDataDetailsDto(int dataId, int companyId, String fileId, int rowNumber, String rowData,
                                   LocalDateTime createdAt) {
}
