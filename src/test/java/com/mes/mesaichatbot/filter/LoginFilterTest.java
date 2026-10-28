package com.mes.mesaichatbot.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("LoginFilter")
@ExtendWith(MockitoExtension.class)
class LoginFilterTest {
    @Mock
    AuthenticationManager authenticationManager;
    @Mock
    AuthenticationSuccessHandler successHandler;
    LoginFilter filter;

    @BeforeEach
    void setUp() {
        filter = new LoginFilter(authenticationManager, successHandler);
    }

    @Test
    @DisplayName("ID와 비밀번호를 JSON 형태로 전송하였을 때 인증에 성공하는가?")
    void authenticatesJsonLoginWithCombinedUserId() {
        var request = new MockHttpServletRequest("POST", "/api/v1/login");
        request.setContentType("application/json");
        request.setContent("{\"companyId\":12,\"employeeId\":34,\"password\":\"pw\"}".getBytes());
        var authenticated = UsernamePasswordAuthenticationToken.authenticated("12_34", "pw", java.util.List.of());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
        assertThat(filter.attemptAuthentication(request, new MockHttpServletResponse())).isSameAs(authenticated);
        var captor = org.mockito.ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("12_34");
        assertThat(captor.getValue().getCredentials()).isEqualTo("pw");
    }

    @Test
    @DisplayName("지원하지 않는 방식과 잘못된 로그인 요청을 거부하는가?")
    void rejectsUnsupportedMethodAndMalformedJson() {
        var get = new MockHttpServletRequest("GET", "/api/v1/login");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> filter.attemptAuthentication(get, new MockHttpServletResponse())).isInstanceOf(org.springframework.security.authentication.AuthenticationServiceException.class);
        var malformed = new MockHttpServletRequest("POST", "/api/v1/login");
        malformed.setContent("not-json".getBytes());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> filter.attemptAuthentication(malformed, new MockHttpServletResponse())).isInstanceOf(RuntimeException.class);
        verifyNoInteractions(authenticationManager);
    }

    @Test
    @DisplayName("인증 성공 결과가 전달되었는가?")
    void delegatesSuccessfulAuthenticationToHandler() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/login");
        var response = new MockHttpServletResponse();
        var authentication = UsernamePasswordAuthenticationToken.authenticated("user", "", java.util.List.of());
        //noinspection DataFlowIssue
        filter.successfulAuthentication(request, response, null, authentication);
        verify(successHandler).onAuthenticationSuccess(request, response, authentication);
    }
}
