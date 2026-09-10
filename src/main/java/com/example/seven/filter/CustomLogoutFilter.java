package com.example.seven.filter;


import com.example.seven.domain.jwt.repository.RefreshRepository;
import com.example.seven.util.JWTUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

public class CustomLogoutFilter extends GenericFilterBean {

    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;

    public CustomLogoutFilter(JWTUtil jwtUtil, RefreshRepository refreshRepository) {
        this.jwtUtil = jwtUtil;
        this.refreshRepository = refreshRepository;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }

    private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException{

        // 로그아웃 API 경로(기존 통신 경로에 맞춰 /api/v1/logout 으로지정)
        String requestUri = request.getRequestURI();
        if(!requestUri.equals("/api/v1/logout")) {
            filterChain.doFilter(request, response);
            return;
        }

        if(!request.getMethod().equals("POST")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 프론트엔드에 발급했던 정확한 쿠키이름("Refresh-Token")으로 추출
        String refresh = null;
        Cookie[] cookies = request.getCookies();
        if(cookies != null) {
            for(Cookie cookie : cookies) {
                if(cookie.getName().equals("Refresh-Token")) {
                    refresh = cookie.getValue();
                }
            }
        }

        if(refresh == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }


        // JWTUtil 구조에 맞게 토큰 검증 로직 수정
        try {
            Claims claims = jwtUtil.getClaims(refresh);
            String tokenType = claims.get("tokenType", String.class);

            if(!"REFRESH".equals(tokenType)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        } catch (ExpiredJwtException e) {
          // 이미 만료된 토큰이여도 브라우저 쿠키는 지워야하므로 반환대신 아래 통과.
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }


        // Redis DB에 저장되어 있는지 확인 후 삭제 진행
        Boolean isExist = refreshRepository.existsById(refresh);
        if (isExist) {
            refreshRepository.deleteById(refresh);
        }

        // 프론트엔드 브라우저의 Refresh-Token 쿠키 값 0으로 초기화
        Cookie cookie = new Cookie("Refresh-Token", null);
        cookie.setMaxAge(0);
        cookie.setPath("/"); // 발급 시점과 동일한 루트 경로로 지정해야 완벽히 삭제
        cookie.setHttpOnly(true); //HttpOnly 옵션 유지


        response.addCookie(cookie);
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
