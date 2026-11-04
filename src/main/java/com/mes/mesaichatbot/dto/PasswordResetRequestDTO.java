package com.mes.mesaichatbot.dto;

public record PasswordResetRequestDTO(
        Long companyId,
        Long employeeId,
        String email
) {
}
