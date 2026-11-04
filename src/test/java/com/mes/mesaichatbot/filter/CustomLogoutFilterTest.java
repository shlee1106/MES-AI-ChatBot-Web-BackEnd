package com.mes.mesaichatbot.filter;

import com.mes.mesaichatbot.repository.RefreshRepository;
import com.mes.mesaichatbot.util.JWTUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("CustomLogoutFilter")
@ExtendWith(MockitoExtension.class)
class CustomLogoutFilterTest {
    @Mock
    JWTUtil jwtUtil;
    @Mock
    RefreshRepository repository;
    ObjectMapper mapper = new ObjectMapper();
    CustomLogoutFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CustomLogoutFilter(jwtUtil, repository, mapper);
    }

    @Test
    @DisplayName("로그아웃 시 Refresh Token을 삭제하고 200이 반환되는가?")
    void deletesExistingRefreshToken() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.get("tokenType", String.class)).thenReturn("REFRESH");
        when(jwtUtil.getClaims("refresh")).thenReturn(claims);
        when(repository.existsById("refresh")).thenReturn(true);
        var request = logoutRequest("{\"refreshToken\":\"refresh\"}");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).isEmpty();
        verify(repository).deleteById("refresh");
    }

    @Test
    @DisplayName("잘못된 로그아웃 요청을 처리하고, 다른 요청은 통과시키는가?")
    void rejectsMissingMalformedOrWrongTypeTokenAndPassesOtherRequests() throws Exception {
        var response = new MockHttpServletResponse();
        filter.doFilter(logoutRequest("{}"), response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).isEmpty();
        response = new MockHttpServletResponse();
        Claims claims = mock(Claims.class);
        when(claims.get("tokenType", String.class)).thenReturn("ACCESS");
        when(jwtUtil.getClaims("access")).thenReturn(claims);
        filter.doFilter(logoutRequest("{\"refreshToken\":\"access\"}"), response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).isEmpty();
        var nextResponse = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/other"),
                        nextResponse, new MockFilterChain());
        assertThat(nextResponse.getStatus()).isEqualTo(200);
        assertThat(nextResponse.getContentAsString()).isEmpty();
        verifyNoInteractions(repository);
    }

    private MockHttpServletRequest logoutRequest(String body) {
        var request = new MockHttpServletRequest("POST", "/api/v1/logout");
        request.setContent(body.getBytes());
        return request;
    }
}
