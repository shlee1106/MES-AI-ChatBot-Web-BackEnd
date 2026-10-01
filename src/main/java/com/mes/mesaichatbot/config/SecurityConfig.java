package com.mes.mesaichatbot.config;

import com.mes.mesaichatbot.domain.jwt.repository.RefreshRepository;
import com.mes.mesaichatbot.filter.CustomLogoutFilter;
import com.mes.mesaichatbot.filter.JWTFilter;
import com.mes.mesaichatbot.filter.LoginFilter;
import com.mes.mesaichatbot.handler.LoginSuccessHandler;
import com.mes.mesaichatbot.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 백엔드 서버의 전체 보안(Security) 규칙을 정의하는 총괄 통제 센터
 * 접근 권한, CORS 설정, JWT 필터 배치 위치 등 여기서 모두 관리
 */
@RequiredArgsConstructor
@Configuration
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final LoginSuccessHandler loginSuccessHandler;
    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;

    /**
     * 로그인을 처리하는 관리자(AuthenticationManager)를 등록
     * LoginFilter가 사용자 아이디, 비번을 DB와 대조할 때 해당 객체 사용
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }


    /**
     * DB에 비밀번호 저장, 비교할 때 사용하는 공식 암호화도구
     * 해커가 DB를 털어도 비밀번호 원본 알 수 없음
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    /**
     * 프론트엔드(Next.js)와 백엔드(Spring) 간의 통신(CORS)을 허락해 주는 설정
     * 브라우저가 다른 도메인과 데이터 주고받아도 되는 지 물을 때 허락해 주는 명부
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 요청을 허락할 프론트엔드 주소 (TODO: 배포 시 프론트 실제 도메인 주소로 반드시 변경!!!!)
        config.setAllowedOriginPatterns(List.of("*")); // TODO: 추후 프론트엔드 서버 IP로 바꾸기
        //config.addAllowedOrigin("http://%s:3000".formatted(hostname)); // TODO: 추후 프론트엔드 서버 IP로 바꾸기

        // 허용할 HTTP 메서드(GET, POST, PUT 등등 모두 허용)
        config.addAllowedMethod("*");

        // 허용할 HTTP 헤더
        config.addAllowedHeader("*");

        // 쿠키(RefreshToken)을 주고받을 수 있도록 허용(매우중요)
        config.setAllowCredentials(true);

        // 프론트엔드의 axios가 응답 헤더에서 AccessToken을 읽을 수 있도록 허용
        config.addExposedHeader("Authorization");


        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 모든 주소("/**")에 대해 위에서 설정한 규칙(config)을 적용
        source.registerCorsConfiguration("/**", config);

        return source;
    }


    /**
     * 권한별 서열 정의(MASTER는 MANAGER권한 다 사용가능, MANAGER는 USER권한 다 사용가능)
     * 자동 상속
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                    ROLE_MASTER > ROLE_MANAGER
                    ROLE_MANAGER > ROLE_USER
                """);
    }

    /**
     * 가장 핵심이 되는 보안 필터 체인 설정
     */
    @Bean
    // [수정] throws Exception 추가
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                // JWT를 사용하므로 기본적 활성화 되는 CSRF 방어막과 기본 로그인 화면 끔
                .csrf(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable)
                // 위에 정의한 CORS(통신 허락) 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));
        // 구역별 출입 규칙
        http.authorizeHttpRequests(auth -> auth

                // 1. 프리패스 구역
                // 로그인, 로그아웃, 재발급, 서버 상태 체크 등
                .requestMatchers("/api/v1/login", "/api/v1/logout", "/api/v1/jwt/refresh", "/error", "/api/v1/", "/actuator/health", "/api/v1/password/**").permitAll()

                // 비밀번호 변경 기능은 로그인 한사람 모두 접근 가능 (해당 비밀번호 변경 기능 개발자가 변경해줄지 유저가 직접 변경할지 의논진행예정)
                //.requestMatchers("/api/v1/password/**").authenticated()

                // 2. 계정 생성 API (중간관리자 이상만 접근 가능)
                // 서열에 의해 MASTER도 자동 통과
                .requestMatchers("/api/v1/admin/users").hasRole("MANAGER") // 계정 생성 가능(MASTER는 중간,하위관리자 생성, MANAGER는 하위관리자만 생성)


                // 3. 권한별 접근 경로
                .requestMatchers("/api/v1/admin/**").hasRole("MASTER") // 최고 관리자 전용
                .requestMatchers("/api/v1/manager/**").hasRole("MANAGER") // 중간 관리자 전용
                .requestMatchers("/api/v1/user/**").hasRole("USER") // 일반 사원 전용

                // 4. 위 적히지 않은 나머지 모든 요청은 무조건 로그인(인증)을 해야 통과
                .anyRequest().authenticated());
        // 필터 배치 순서 설정(매우중요)
        http
                // 기존 로그아웃 필터가 작동하기 전, 내가 만든 CustomLogoutFilter(쿠키 삭제 등) 먼저 실행
                .addFilterBefore(new CustomLogoutFilter(jwtUtil, refreshRepository, new ObjectMapper()), org.springframework.security.web.authentication.logout.LogoutFilter.class)

                // 기본 아이디,비번 검사 필터 앞에, 내가 만든 JWTFilter(토큰 검사기) 먼저 배치(모든 요청에서 토큰 부터 검사)
                .addFilterBefore(new JWTFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)

                // 기본 아이디, 비번 검사 필터 자리를 내가 만든 LoginFilter(JSON 로그인 처리기)로 갈아 끼움
                .addFilterAt(new LoginFilter(authenticationManager(authenticationConfiguration), loginSuccessHandler), UsernamePasswordAuthenticationFilter.class);
        // JWT 특화 설정(무상태성)
        http.sessionManagement(session -> session
                // 서버의 메모리(세션)에 유저 정보를 기억하지 않겠다고 선언(STATELESS)
                // 오직 요청 헤더에 담긴 JWT만 보고 판단
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 위 보안 시스템 조립 후 시스템 가동
        return http.build();
    }
}