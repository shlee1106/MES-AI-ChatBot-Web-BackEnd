package com.mes.mesaichatbot.domain.user.dto;

public record LoginRequestDTO(
        Long companyId,
        Long employeeId,
        String password
) {
    
}
