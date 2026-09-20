package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.domain.email.dto.PasswordResetRequestDTO;
import com.mes.mesaichatbot.domain.email.dto.PasswordUpdateRequestDTO;
import com.mes.mesaichatbot.domain.email.serivce.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/password")
@RequiredArgsConstructor
public class EmailController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/request")
    public ResponseEntity<String> requestResetEmail(@RequestBody PasswordResetRequestDTO dto) {
        passwordResetService.sendRestEmail(dto);
        return ResponseEntity.ok("비밀번호 재설정 이메일이 성공적으로 발송되었습니다.");
    }

    @PostMapping("/update")
    public ResponseEntity<String> updatePassword(
            @RequestBody PasswordUpdateRequestDTO dto) {

        if (dto.token() == null) {
            return ResponseEntity.badRequest().body("토큰이 만료되었거나 존재하지 않습니다.");
        }

        passwordResetService.updatePassword(dto.token(), dto.newPassword());
        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }
}
