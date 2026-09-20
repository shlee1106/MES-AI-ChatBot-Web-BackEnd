package com.mes.mesaichatbot.domain.email.dto;

public record PasswordResetRequestDTO(
        Long companyId,
        Long employeeId,
        String email
) {
}
