package com.mes.mesaichatbot.domain.user.dto;

import com.mes.mesaichatbot.domain.user.entity.UserRole;

public record UserInfoResponseDTO(
        Long companyId,
        Long employeeId,
        String username,
        String email,
        UserRole role
) {}
