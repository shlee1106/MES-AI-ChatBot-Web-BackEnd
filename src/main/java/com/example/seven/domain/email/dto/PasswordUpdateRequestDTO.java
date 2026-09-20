package com.example.seven.domain.email.dto;

public record PasswordUpdateRequestDTO(
        String token,
        String newPassword
) {
}
