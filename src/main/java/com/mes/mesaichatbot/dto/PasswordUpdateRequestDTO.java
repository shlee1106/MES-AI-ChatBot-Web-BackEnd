package com.mes.mesaichatbot.dto;

public record PasswordUpdateRequestDTO(
        String token,
        String newPassword
) {
}
