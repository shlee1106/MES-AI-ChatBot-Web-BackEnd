package com.mes.mesaichatbot.dto;

public record LoginRequestDTO(
        Long companyId,
        Long employeeId,
        String password
) {

}
