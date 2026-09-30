package com.mes.mesaichatbot.dto;

import com.mes.mesaichatbot.domain.user.entity.UserRole;

public record UserMemberResponseDto(Long companyId, Long employeeId, String companyName, String username, String email,
                                    UserRole role) {
}
