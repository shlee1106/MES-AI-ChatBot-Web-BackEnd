package com.example.seven.domain.user.dto;

import com.example.seven.domain.user.entity.UserRole;

public record UserInfoResponseDTO(
        Long companyId,
        Long employeeId,
        String username,
        String email,
        UserRole role
) {}
