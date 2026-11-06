package com.mes.mesaichatbot.handler;


import com.mes.mesaichatbot.domain.RefreshEntity;
import com.mes.mesaichatbot.repository.RefreshRepository;
import com.mes.mesaichatbot.util.JWTUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;

    public LoginSuccessHandler(JWTUtil jwtUtil, RefreshRepository refreshRepository) {
        this.jwtUtil = jwtUtil;
        this.refreshRepository = refreshRepository;
    }


    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String username = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        String accessToken = jwtUtil.createAccessToken(username, role);
        String refreshToken = jwtUtil.createRefreshToken(username, role);

        RefreshEntity refreshEntity = new RefreshEntity();
        refreshEntity.setUsername(username);
        refreshEntity.setRefresh(refreshToken);
        refreshRepository.save(refreshEntity);

        // Access Token: 포론트엔드가 사용할 수 있도록 헤더로 전달
        response.setHeader("Authorization", "Bearer " + accessToken);

        // Refresh Token: 자바스크립트 접근이 원찬 차단되는 HttpOnly 쿠키로 굽기
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                                                     .path("/")
                                                     .httpOnly(true)
                                                     .secure(true)
                                                     .sameSite("Lax")
                                                     .maxAge(30 * 24 * 60 * 60)
                                                     .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // Body: 로그인 성공시 로그인 성공 메세지와 refreshToken body에 출력

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"Login Success\"}");
//        response.getWriter().write("{\"message\":\"Login Success\", \"refreshToken\":\"%s\"}".formatted(refreshToken));
        response.getWriter().flush();
    }
}
