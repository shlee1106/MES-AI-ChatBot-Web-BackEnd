package com.mes.mesaichatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PasswordUpdateRequestDTO(
        @NotBlank(message = "토큰은 필수입니다.")
        @NotNull
        String token,
        @NotNull
        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[@$!%*#?&])[A-Za-z0-9@$!%*#?&]{8,256}$",
                message = "비밀번호는 8-256자리여야 하며, 영문, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다.")
        String newPassword
) {
}
