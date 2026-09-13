package com.example.seven.config;

import com.example.seven.domain.jwt.repository.RefreshRepository;
import com.example.seven.filter.CustomLogoutFilter;
import com.example.seven.filter.JWTFilter;
import com.example.seven.filter.LoginFilter;
import com.example.seven.handler.LoginSuccessHandler;
import com.example.seven.util.JWTUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final LoginSuccessHandler loginSuccessHandler;
    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;

    public SecurityConfig(AuthenticationConfiguration authenticationConfiguration, LoginSuccessHandler loginSuccessHandler, JWTUtil jwtUtil, RefreshRepository refreshRepository) {
        this.authenticationConfiguration = authenticationConfiguration;
        this.jwtUtil = jwtUtil;
        this.loginSuccessHandler = loginSuccessHandler;
        this.refreshRepository = refreshRepository;
    }

    @Bean
    // throws Exception 추가
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    // 시스템의 공식 비밀번호 암호화 도구(MySQL)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // TODO: 추후 배포 시 수정
        config.setAllowedOriginPatterns(java.util.List.of("*")); // 모든 포트 허용(수정필요)
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);

        // [추가] 프론트엔드가 응답 헤더의 토큰을 읽을 수 있도록 허용
        config.addExposedHeader("Authorization");
        

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }


    // 권한별 서열 정의
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_MASTER > ROLE_MANAGER
                ROLE_MANAGER > ROLE_USER
                """);
    }

    @Bean
    // [수정] throws Exception 추가
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(login -> login.disable());

        // 권한 기반 계층 규칙 적용
        http
                .authorizeHttpRequests(auth -> auth

                        // 1. 인증 불필요 경로
                        .requestMatchers("/api/v1/login", "/api/v1/logout", "/api/v1/jwt/refresh", "/error", "/api/v1/").permitAll() //로그인, 로그아웃, refresh토큰인해 재발급, 최상위경로 접근 가능
                        .requestMatchers("/api/v1/password/**").permitAll() // 비밀번호 수정 관련 접근 가능

                        // 2. 계정 생성 API (최고관리자, 중간관리자 모두 허용)
                        // 최소 요구 권한을 MANAGER로 설정 (서열에 의해 MASTER도 자동 통과)
                        .requestMatchers("/api/v1/admin/users").hasRole("MANAGER") // 계정 생성 가능(MASTER는 중간,하위관리자 생성, MANAGER는 하위관리자만 생성)



                        // 3. 권한별 접근 경로
                        .requestMatchers("/api/v1/admin/**").hasRole("MASTER") //admin 경로는 MASTER 권한만 접근 가능
                        .requestMatchers("/api/v1/manager/**").hasRole("MANAGER") //manager 경로는 MASTER, MANAGER만 접근 가능
                        .requestMatchers("/api/v1/user/**").hasRole("USER") //user 경로엔 모두 다 접근 가능

                        // 4. 나머지 요청은 로그인 필요
                        .anyRequest().authenticated()
                );

        // 필터 체인 설정 부분에 로그아웃 필터 추가
        http
                .addFilterBefore(new CustomLogoutFilter(jwtUtil, refreshRepository), org.springframework.security.web.authentication.logout.LogoutFilter.class)
                .addFilterBefore(new JWTFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)
                .addFilterAt(new LoginFilter(authenticationManager(authenticationConfiguration), loginSuccessHandler), UsernamePasswordAuthenticationFilter.class);
        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}