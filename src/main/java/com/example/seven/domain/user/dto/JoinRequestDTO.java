// 회원가입 요청용 (UserService에서 사용)
package com.example.seven.domain.user.dto;

import com.example.seven.domain.user.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record JoinRequestDTO(

        @NotNull(message = "회사 번호는 필수 입력값입니다.")
        Long companyId,

        @NotNull(message = "사원 번호는 필수 입력값입니다.")
        Long employeeId,

        @NotBlank(message = "비밀번호는 필수 입력값입니다.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[@$!%*#?&])[A-Za-z0-9@$!%*#?&]{8,20}$",
                message = "비밀번호는 8~20자리이며, 영문, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다.")
        String password,

        @NotBlank(message = "이름은 필수 입력값입니다.")
        String username, // 사용자의 실제 이름

        @NotBlank(message = "이메일은 필수 입력값입니다.")
        @Email(message = "유요한 이메일 형식이 아닙니다.")
        String email,

        @NotNull(message = "권한은 필수 입력값입니다.")
        UserRole role
) {}