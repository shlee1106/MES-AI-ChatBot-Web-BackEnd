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
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

/**
 * 프론트엔드에서 로그아웃(POST /api/v1/logout)을 요청했을 때 이를 가로채서 처리하는 전용 필터
 * DB(Redis)에 저장된 RefreshToken을 폐기하며 프론트엔드 브라우저의 쿠키를 삭제 수행
 */
@RequiredArgsConstructor
public class CustomLogoutFilter extends GenericFilterBean {

    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // HTTP 전용 객체로 변환하여 실제 로그아웃 로직(아래 메서드)으로 넘겨줌
        doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }

    private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException{

        // [경로 검사] 프론트엔드가 정확히 "/api/v1/logout" 경로로 요청했는지 확인
        String requestUri = request.getRequestURI();
        if(!requestUri.equals("/api/v1/logout")) {
            // 로그아웃 요청이 아니면 이 필터는 아무 일 하지 않고 다음 필터로 통과
            filterChain.doFilter(request, response);
            return;
        }

        // [메소드 검사] 로그아웃은 반드시 POST 방식으로만 처리
        if(!request.getMethod().equals("POST")) {
            filterChain.doFilter(request, response);
            return;
        }

        // [쿠키 수색] 프론트엔드 브라우저가 보낸 쿠키 목록을 뒤져서 "Refresh-Token"을 찾아냄
        String refresh = null;
        Cookie[] cookies = request.getCookies();
        if(cookies != null) {
            for(Cookie cookie : cookies) {
                if(cookie.getName().equals("Refresh-Token")) {
                    refresh = cookie.getValue();
                }
            }
        }

        // 애초에 쿠키에 RefreshToken이 없다면 로그아웃할 것도 없으므로 400 에러 반환
        if(refresh == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }


        // [토큰 검증] 찾아낸 Refresh Token이 우리가 발급한 게 맞는지 확인
        try {
            Claims claims = jwtUtil.getClaims(refresh);
            String tokenType = claims.get("tokenType", String.class);

            // AccessToken등 엉뚱한 토큰 가져오면 차단
            if(!"REFRESH".equals(tokenType)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        } catch (ExpiredJwtException e) {
          // [핵심] 토큰의 기한이 이미 만료되었더라도 튕겨내면 안됨
          // 목표는 완벽한 로그아웃(잔여 쿠키 삭제)이므로 에러를 무시하고 아래 로직으로 통과
        } catch (Exception e) {
            // 서명이 조작된 토큰이라면 400에러 반환
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }


        // [DB(Redis) 삭제] 백엔드 저장소에 해당 토큰이 남았는지 확인하고 있다면 영구 삭제
        // 해당 과정을 거쳐야 해커가 훔쳐간 토큰을 나중에 몰래 재발급 시도 원천 차단
        Boolean isExist = refreshRepository.existsById(refresh);
        if (isExist) {
            refreshRepository.deleteById(refresh);
        }

        // [브라우저 쿠키 삭제] 프론트엔드 브라우저에 남아있는 쿠키를 완전히 삭제
        // 방법으론 동일한 이름("Refresh-Token")의 쿠키를 새로 만들되, 수명을 0초로 설정해 덮어씀
        Cookie cookie = new Cookie("Refresh-Token", null);
        cookie.setMaxAge(0);
        cookie.setPath("/"); // 쿠키를 구웠을 때와 똑같은 경로로 지정해야 브라우저가 정확히 지움
        cookie.setHttpOnly(true); //HttpOnly 옵션 유지 (보안 규격 맞춤)

        // 0초짜리 소멸용 쿠키를 응답에 담아 프론트엔드로 보내고, 성공(200) 상태 코드 반환
        response.addCookie(cookie);
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
