package com.example.seven.config;

import com.example.seven.domain.jwt.repository.RefreshRepository;
import com.example.seven.filter.CustomLogoutFilter;
import com.example.seven.filter.JWTFilter;
import com.example.seven.filter.LoginFilter;
import com.example.seven.handler.LoginSuccessHandler;
import com.example.seven.util.JWTUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    @Value("${HOSTNAME:localhost}")
    private String hostname;

    public SecurityConfig(AuthenticationConfiguration authenticationConfiguration, LoginSuccessHandler loginSuccessHandler, JWTUtil jwtUtil, RefreshRepository refreshRepository) {
        this.authenticationConfiguration = authenticationConfiguration;
        this.jwtUtil = jwtUtil;
        this.loginSuccessHandler = loginSuccessHandler;
        this.refreshRepository = refreshRepository;
    }

    @Bean
    // [수정] throws Exception 추가
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

        config.addAllowedOrigin("*"); // TODO: 추후 프론트엔드 서버 IP로 바꾸기
//        config.addAllowedOrigin("http://%s:3000".formatted(hostname)); // TODO: 추후 프론트엔드 서버 IP로 바꾸기
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);

        // [추가] 프론트엔드가 응답 헤더의 토큰을 읽을 수 있도록 허용
        config.addExposedHeader("Authorization");
        

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    @Bean
    // [수정] throws Exception 추가
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(login -> login.disable());

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/join").permitAll()
                        .requestMatchers("/api/v1/jwt/refresh").permitAll()
                        .requestMatchers("/api/v1/login").permitAll()
                        .requestMatchers("/error").permitAll()
                        // [추가] 비밀번호 재설정 관련 API 인증 없이 접근 허용
                        .requestMatchers("/api/v1/password/**").permitAll()

                        .requestMatchers("/api/v1/user").hasRole("USER")
                        .anyRequest().hasRole("ADMIN")
                );

        // JWTFilter를 LoginFilter보다 앞단(Before)으로 배치
        http
                .addFilterBefore(new JWTFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)
                .addFilterAt(new LoginFilter(authenticationManager(authenticationConfiguration), loginSuccessHandler), UsernamePasswordAuthenticationFilter.class);

        // 필터 체인 설정 부분에 로그아웃 필터 추가
        http
                .addFilterBefore(new CustomLogoutFilter(jwtUtil, refreshRepository), org.springframework.security.web.authentication.logout.LogoutFilter.class)
                .addFilterBefore(new JWTFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}