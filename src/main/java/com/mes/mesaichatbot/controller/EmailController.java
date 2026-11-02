package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.domain.email.dto.PasswordResetRequestDTO;
import com.mes.mesaichatbot.domain.email.dto.PasswordUpdateRequestDTO;
import com.mes.mesaichatbot.domain.email.serivce.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 이메일 관련 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/password")
@RequiredArgsConstructor
public class EmailController {
    private final PasswordResetService passwordResetService;

    /**
     * 비밀번호 재설정 이메일을 전송합니다.
     *
     * @param dto {@link PasswordResetRequestDTO} 사용자의 재설정 요청 DTO.
     * @return {@link ResponseEntity<String>} 비밀번호 재설정 요청 결과.
     */
    @PostMapping(path = "/request",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> requestResetEmail(@RequestBody PasswordResetRequestDTO dto) {
        // 이메일 전송 시도
        try {
            passwordResetService.sendResetEmail(dto);
            return ResponseEntity.ok("비밀번호 재설정 이메일이 성공적으로 발송되었습니다.");
        } catch (IllegalArgumentException e) {
            // 잘못된 데이터 수신 시 400
            log.warn(e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 토큰 유효성 검증을 수행합니다.
     *
     * @param token 토큰
     * @return {@link ResponseEntity<Boolean>} 토큰의 유효성 여부.
     */
    @GetMapping(path = "/token/{token}")
    public ResponseEntity<Boolean> verifyToken(@PathVariable String token) {
        try {
            // 토큰의 유효성 검증
            passwordResetService.verifyTokenExists(token);

            // 토큰이 유효하면 true 반환
            return ResponseEntity.ok(true);
        } catch (IllegalArgumentException e) {
            // 토큰이 유효하지 않으면 404
            log.warn(String.valueOf(e));
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 토큰을 통해 비밀번호를 재설정합니다.
     *
     * @param dto {@link PasswordUpdateRequestDTO} 비밀번호 재설정 요청 DTO.
     * @return {@link ResponseEntity<String>} 비밀번호 재설정 결과.
     */
    @PostMapping(path = "/update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> updatePassword(@RequestBody PasswordUpdateRequestDTO dto) {
        // 제공된 토큰이 없거나, 새 비밀번호가 없으면 400
        if (dto.token() == null) {
            return ResponseEntity.badRequest().body("토큰이 만료되었거나 존재하지 않습니다.");
        }
        if (dto.newPassword() == null || dto.newPassword().isBlank()) {
            return ResponseEntity.badRequest().body("비밀번호가 설정되지 않았습니다.");
        }

        // 비밀번호 재설정 시도
        try {
            passwordResetService.updatePassword(dto.token(), dto.newPassword());
            return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            // 잘못된 데이터 수신 시 400
            log.warn(e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
