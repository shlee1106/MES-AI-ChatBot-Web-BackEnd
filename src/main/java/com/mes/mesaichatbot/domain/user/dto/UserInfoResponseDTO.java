package com.mes.mesaichatbot.domain.user.dto;

import com.mes.mesaichatbot.domain.Company;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.domain.user.entity.UserRole;

import java.util.List;

public record UserInfoResponseDTO(
        Long companyId,
        Long employeeId,
        String username,
        String email,
        UserRole role,
        String companyName
) {
    /**
     * 사용자 엔티티를 DTO로 변환합니다.
     *
     * @return {@link UserInfoResponseDTO}.
     */
    public static UserInfoResponseDTO from(UserMember member) {
        return new UserInfoResponseDTO(
                member.getCompanyId().getCompanyId(), member.getEmployeeId(),
                member.getUsername(), member.getCompanyEmail(),member.getRoleCode(), member.getCompanyId().getCompanyName());
    }

    public static List<UserInfoResponseDTO> from(List<UserMember> member) {
        return member.stream().map(UserInfoResponseDTO::from).toList();
    }
}
