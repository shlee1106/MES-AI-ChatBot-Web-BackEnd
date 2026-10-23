package com.mes.mesaichatbot.filter;

import com.mes.mesaichatbot.util.JWTUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;

    @Override
    // 필터를 거칠 필요 없는 API모음 메소드
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 실제 로그인, 로그아웃, 재발급 API 경로에 맞게 수정
        return path.equals("/api/v1/login")
                || path.equals("/api/v1/jwt/refresh")
                || path.equals("/api/v1/join")
                || path.equals("/api/v1/logout");
    }

    @Override
    /**
     * WhiteList(shouldNotFilter)에 포함되지 않은 모든 API 요청이 무조건 거쳐가는 핵심 검문소
     * 프론트엔드가 보낸 Access Token의 위조 및 만료 여부를 꼼꼼히 검사하고,
     * 통과 시 SecurityContext에 인증 정보(방문증)를 걸어둠.
     */
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 프론트엔드가 보낸 요청(Header)에서 "Authorization"키에 담긴 값 꺼냄
        String authorization = request.getHeader("Authorization");

        // 헤더에 토큰이 없을 경우
        if (authorization == null) {
            // 다음 필터로 넘김 (로그인이 필수인 API라면 Security 인가 필터가 자동으로 튕겨냄)
            filterChain.doFilter(request, response);
            return;
        }

        // 토큰이 있지만 지정한 "Bearer" 포맷이 아닌 경우(잘못된 요청)
        if (!authorization.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 권한 없음
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"유효하지 않은 토큰\"}");
            return;
        }

        // 정상적인 포맷이면 앞에 붙은 "Bearer" 떼어내고 순수 JWT 문자열만 추출
        String accessToken = authorization.substring(7);


        try {
            //JWTUtil의 감별기를 통해 위조 및 만료 여부 깐깐히 검사 후 내용물(Claims) 꺼냄
            // 문제시 catch 블록으로 바로 이동
            Claims claims = jwtUtil.getClaims(accessToken);

            // 내용물에서 유저 식별자(회사번호_사원번호), 권한, 토큰 종류 추출
            String username = claims.getSubject();
            String role = claims.get("role", String.class);
            String tokenType = claims.get("tokenType", String.class);

            //[중요] 해당 토큰이 AccessToken이 맞는 지 다시 한번 더 확인
            // 누군가 수명이 긴 RefreshToken을 헤더에 넣어 접근 시도 원천 차단
            if (!"ACCESS".equals(tokenType)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"유효하지 않은 토큰\"}");
                return;
            }

            // 위 검문 통과시 추출한 권한 스프링 시큐리티 공식 규격으로 포장
            List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));

            // 최종 공식 방문증 발급(해당 방문증엔 "1_2000" 같은 식별자가 있어 컨트롤러에서 언제든 꺼낼 수 있음"
            Authentication auth = new UsernamePasswordAuthenticationToken(username, null, authorities);

            // 백엔드 서버 전역 상황판(SecurityContext)에 방문증 걸어둠
            SecurityContextHolder.getContext().setAuthentication(auth);

            // 모든 인증 절차 끝나 다음 필터나 목적지(컨트롤러)로 요청을 무사히 통과
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            //[에러 처리1] 토큰 수명이 끝났을 때 터지는 에러
            // 프론트엔드의 ky 인터셉터가 해당 401 상태 코드,메세지를 보고 몰래 재발급(/refresh)
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"토큰이 만료되었습니다.\"}");
        } catch (io.jsonwebtoken.JwtException e) {
            //[에러 처리2] 서명이 틀리거나 훼손(위조)된 토큰일 경우 터지는 에러
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"유효하지 않은 토큰입니다.\"}");
        }
    }
}
