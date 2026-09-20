package com.example.seven.filter;

import com.example.seven.domain.user.dto.JoinRequestDTO;
import com.example.seven.domain.user.dto.LoginRequestDTO;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


/**
 * 프론트엔드에서 날라오는 JSON형태의 로그인 요청을 가로채 처리하는 전용 로그인 필터
 * Security에서 제공하는 기본 폼 로그인 기능 끄고 해당 필터로 대체 사용
 */
public class LoginFilter extends AbstractAuthenticationProcessingFilter {

    // 해당 필터가 작동할 정확한 조건 설정
    // 무조건 POST방식이어야 하며, 주소가 "/api/v1/login"일 때만 해당 필터가 낚아챔
    private static final RequestMatcher DEFAULT_ANT_PATH_REQUEST_MATCHER = PathPatternRequestMatcher.pathPattern(HttpMethod.POST,"/api/v1/login");

    // 로그인 성공했을 때 JWT 토큰을 발급해 줄 담당자
    private final AuthenticationSuccessHandler authenticationSuccessHandler;

    // 필터 생성자 : SecurityConfig에서 이 필터 등록할 때 비밀번호 검증기(Manager)와 성공 처리기(Handler) 주입
    public LoginFilter(
            AuthenticationManager authenticationManager,
            AuthenticationSuccessHandler authenticationSuccessHandler
    ) {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER, authenticationManager);
        this.authenticationSuccessHandler = authenticationSuccessHandler;
    }

    /**
     * 프론트엔드가 로그인 버튼을 누르면 가장 먼저 실행되는 핵심 메서드
     * 아이디와 비밀번호를 꺼내 임시 출입증 생성 후 검증 요청
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        // 혹시라도 GET 등의 다른 방식으로 로그인 시도 시 에러반환 후 차단
        if (!request.getMethod().equals("POST")) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        }

        try {
            // ObjectMapper: 프론트엔드가 보낸 JSON 문자열을 Java객체(DTO)로 변환해주는 도구
            ObjectMapper objectMapper = new ObjectMapper();

            // HTTP 요청 Body에 담긴 JSON 데이터를 순수한 문자열로 읽어옴
            ServletInputStream inputStream = request.getInputStream();
            String messageBody = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);

            // 읽어온 문자열을 미리 만들어둔 LoginRequestDTO 껍데기에 담아줌
            LoginRequestDTO loginDTO = objectMapper.readValue(messageBody, LoginRequestDTO.class);

            // DTO에서 로그인에 필요한 정보만 뽑아냄
            Long companyId = loginDTO.companyId();
            Long employeeId = loginDTO.employeeId();
            String password = loginDTO.password();

            //[핵심로직] 회사 로그인 식별 규칙
            // 회사번호와 사원번호를 조합해 "1_2000" 형태의 유일한 고유 아이디로 생성
            // 해당 문자열은 UserService.loadUserByUsername()메소드로 넘어가 다시 분해되어 DB 조회로 쓰임
            String combinedUsername = companyId + "_" + employeeId;

            // 갓 만들어진 미인증 상태의 임시 출입증 생성
            // (아직까진 비밀번호가 맞는 지 모르는 상태)
            UsernamePasswordAuthenticationToken authRequest =
                    UsernamePasswordAuthenticationToken.unauthenticated(combinedUsername, password);

            // 요청자의 IP 주소나 세션 ID 같은 부가 정보(Details)를 임시 출입증에 꼬리표처럼 달아줌
            setDetails(request, authRequest);

            // AuthenticationManager(최종 심사관)에게 임시 출입증 제출
            // 비밀번호 맞는지 확인 요청
            // (비밀번호 틀릴 시 여기서 자동으로 에러가 반환되어 로그인 실패 처리)
            return this.getAuthenticationManager().authenticate(authRequest);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 임시 출입증에 부가적인 웹 요청 정보(IP 등)를 세팅해주는 보조 메서드
     */
    protected void setDetails(HttpServletRequest request, UsernamePasswordAuthenticationToken authRequest) {
        authRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));
    }

    /**
     * AuthenticationManager의 깐깐한 비밀번호 심사를 무사히 통과 하였을 때만 실행되는 메서드
     * 로그인 최종 성공 상태
     */
    @Override
    protected void successfulAuthentication(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain,
                                            @NonNull Authentication authResult) throws IOException, ServletException {
        authenticationSuccessHandler.onAuthenticationSuccess(request, response, authResult);
    }
}