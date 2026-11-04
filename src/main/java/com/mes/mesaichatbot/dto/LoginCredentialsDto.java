package com.mes.mesaichatbot.dto;

public record LoginCredentialsDto(
        String id,
        String accessToken,
        String refreshToken
) {
}
