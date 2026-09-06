package com.mes.mesaichatbot.dto;

public record AuthResponseDto(
        String status,
        String message,
        long companyId,
        LoginCredentialsDto loginData
) {
}
