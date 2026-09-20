package com.mes.mesaichatbot.domain.email.dto;

public record PasswordUpdateRequestDTO(
        String token,
        String newPassword
) {
}
