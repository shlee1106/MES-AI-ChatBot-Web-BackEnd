package com.example.seven.api;

import com.example.seven.domain.email.dto.PasswordResetRequestDTO;
import com.example.seven.domain.email.dto.PasswordUpdateRequestDTO;
import com.example.seven.domain.email.serivce.PasswordResetService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

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

    @GetMapping("/verify")
    public void verifyTokenAndRedirect(@RequestParam String token, HttpServletResponse response) throws IOException {
        passwordResetService.verifyTokenExists(token);

        Cookie cookie = new Cookie("RESET_TOKEN",token);
        cookie.setHttpOnly(true);
        cookie.setPath("/"); // 개발 완료된 후 배포시 /api/v1/password/update 주소로 변경
        cookie.setMaxAge(300);

        response.addCookie(cookie);
        response.sendRedirect("http://localhost:3000/reset-pw");
    }

    @PostMapping("/update")
    public ResponseEntity<String> updatePassword(
            @CookieValue(value = "RESET_TOKEN", required = false) String token,
            @RequestBody PasswordUpdateRequestDTO dto) {

        if (token == null) {
            return ResponseEntity.badRequest().body("토큰이 만료되었거나 존재하지 않습니다.");
        }

        passwordResetService.updatePassword(token, dto.newPassword());
        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }
}
