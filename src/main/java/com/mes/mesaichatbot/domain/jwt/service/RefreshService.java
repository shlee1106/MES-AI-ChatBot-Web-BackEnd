package com.mes.mesaichatbot.domain.jwt.service;

import com.mes.mesaichatbot.domain.jwt.repository.RefreshRepository;
import com.mes.mesaichatbot.util.JWTUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RefreshService {

    private final RefreshRepository refreshRepository;
    private final JWTUtil jwtUtil;

    public RefreshService(RefreshRepository refreshRepository, JWTUtil jwtUtil) {
        this.refreshRepository = refreshRepository;
        this.jwtUtil = jwtUtil;
    }

    public String refresh(String refreshToken) {

        // Refresh 토큰 검증
        Claims claims;
        try{
            claims = jwtUtil.getClaims(refreshToken);
        } catch (Exception e) {
            log.error(String.valueOf(e));
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");

        }

        String tokenType = claims.get("tokenType", String.class);

        if(!"REFRESH".equals(tokenType)) {
            throw new IllegalArgumentException("Refresh Token이 아닙니다.");
        }

        // 화이트 리스트 확인
        boolean exists = refreshRepository.existsById(refreshToken);

        if(!exists) {
            throw new IllegalArgumentException("로그아웃되었거나 만료된 Refresh Token입니다.");
        }

        // 재발급
        String username = claims.getSubject();
        String role = claims.get("role", String.class);

        return jwtUtil.createAccessToken(username, role);
    }
}
