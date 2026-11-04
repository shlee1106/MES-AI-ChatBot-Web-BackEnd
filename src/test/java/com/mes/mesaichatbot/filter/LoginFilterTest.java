package com.mes.mesaichatbot.filter;

import com.mes.mesaichatbot.config.MockMvcTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@WebMvcTest(LoginFilter.class)
@Import(MockMvcTestConfig.class)
class LoginFilterTest {
    @MockitoBean
    AuthenticationManager authenticationManager;
    @MockitoBean
    AuthenticationSuccessHandler successHandler;
    LoginFilter filter;

    @BeforeEach
    void setUp() {
        filter = new LoginFilter(authenticationManager, successHandler);
    }

    @Test
    @DisplayName("ID와 비밀번호를 JSON 형태로 전송하였을 때 인증에 성공하는가?")
    void authenticatesJsonLoginWithCombinedUserId() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/login");
        request.setContentType("application/json");
        request.setContent("{\"companyId\":12,\"employeeId\":34,\"password\":\"pw\"}".getBytes());
        UsernamePasswordAuthenticationToken authenticated = UsernamePasswordAuthenticationToken
                .authenticated("12_34", "pw", List.of());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
        assertThat(filter.attemptAuthentication(request, new MockHttpServletResponse())).isSameAs(authenticated);
        ArgumentCaptor<Authentication> captor = org.mockito.ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("12_34");
        assertThat(captor.getValue().getCredentials()).isEqualTo("pw");
    }

    @Test
    @DisplayName("지원하지 않는 방식과 잘못된 로그인 요청을 거부하는가?")
    void rejectsUnsupportedMethodAndMalformedJson() {
        MockHttpServletRequest get = new MockHttpServletRequest("GET", "/api/v1/login");
        org.assertj.core.api.Assertions.assertThatThrownBy(
                   () -> filter.attemptAuthentication(get, new MockHttpServletResponse()))
                                       .isInstanceOf(AuthenticationServiceException.class);
        MockHttpServletRequest malformed = new MockHttpServletRequest("POST", "/api/v1/login");
        malformed.setContent("not-json".getBytes());
        org.assertj.core.api.Assertions.assertThatThrownBy(
                   () -> filter.attemptAuthentication(malformed, new MockHttpServletResponse()))
                                       .isInstanceOf(RuntimeException.class);
        verifyNoInteractions(authenticationManager);
    }

    @Test
    @DisplayName("인증 성공 결과가 전달되었는가?")
    void delegatesSuccessfulAuthenticationToHandler() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken
                .authenticated("user", "", java.util.List.of());
        //noinspection DataFlowIssue
        filter.successfulAuthentication(request, response, null, authentication);
        verify(successHandler).onAuthenticationSuccess(request, response, authentication);
    }
}
