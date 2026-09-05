// 회원가입 요청용 (UserService에서 사용)
package com.example.seven.domain.user.dto;

public record JoinRequestDTO(
        Long companyId,
        Long employeeId,
        String password,
        String username, // 사용자의 실제 이름
        String email
) {}