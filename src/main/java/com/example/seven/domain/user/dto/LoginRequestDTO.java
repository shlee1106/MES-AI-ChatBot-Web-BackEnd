package com.example.seven.domain.user.dto;

public record LoginRequestDTO(
        Long companyId,
        Long employeeId,
        String password
) {
    
}
