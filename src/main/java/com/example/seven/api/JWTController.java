package com.example.seven.api;

import com.example.seven.domain.jwt.service.RefreshService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class JWTController {

    private final RefreshService refreshService;

    public JWTController(RefreshService refreshService) {
        this.refreshService = refreshService;
    }

    @PostMapping("/jwt/refresh")
    public ResponseEntity<?> refresh(
            // 1. 바디가 아닌 HttpOnly 쿠키에서 Refresh-Token을 바로 꺼냄.
            @CookieValue(name = "Refresh-Token", required = false) String refreshToken
    ) {
        // 2. 쿠키가 비어있으면 401(권한 없음) 에러를 반환합니다.
        if (refreshToken == null) {
            return new ResponseEntity<>("Refresh Token이 존재하지 않습니다.", HttpStatus.UNAUTHORIZED);
        }

        try {
            // 3. 기존에 만들어두신 RefreshService를 그대로 활용하여 새 토큰을 발급.
            String newAccessToken = refreshService.refresh(refreshToken);

            // 4. 프론트엔드의 인터셉터가 읽을 수 있도록 응답 헤더에 새 토큰을 꽂아줌.
            return ResponseEntity.ok()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken)
                    .body("토큰 재발급 성공");

        } catch (Exception e) {
            // 서비스 로직에서 토큰 만료 등의 에러가 발생하면 401 에러를 반환함.
            return new ResponseEntity<>("토큰 갱신 실패: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }
}