package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.domain.jwt.service.RefreshService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class JWTController {

    private final RefreshService refreshService;

    @PostMapping("/jwt/refresh")
    public ResponseEntity<String> refresh(@CookieValue("refreshToken") String refreshToken) {
        if (refreshToken == null) {
            return new ResponseEntity<>("Refresh Token이 존재하지 않습니다.", HttpStatus.UNAUTHORIZED);
        }

        try {
            // 3. 기존에 만들어둔 RefreshService를 그대로 활용하여 새 토큰을 발급.
            String newAccessToken = refreshService.refresh(refreshToken);

            // 4. 프론트엔드의 인터셉터가 읽을 수 있도록 응답 헤더에 새 토큰을 꽂아줌.
            return ResponseEntity.ok().header(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken).body("토큰 재발급 성공");

        } catch (Exception e) {
            // 서비스 로직에서 토큰 만료 등의 에러가 발생하면 401 에러를 반환함.
            return new ResponseEntity<>("토큰 갱신 실패: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }
}