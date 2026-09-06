package com.mes.mesaichatbot.dto;

public record AuthRequestDto(
        long companyId,
        LoginDataDto loginData
) {
}
