package com.example.seven.domain.email.dto;

public record PasswordResetRequestDTO(
        Long companyId,
        Long employeeId,
        String email
) {
}
