package com.mes.mesaichatbot.filter;

import com.mes.mesaichatbot.repository.RefreshRepository;
import com.mes.mesaichatbot.util.JWTUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@WebMvcTest(CustomLogoutFilter.class)
class CustomLogoutFilterTest {
    @MockitoBean
    JWTUtil jwtUtil;
    @MockitoBean
    RefreshRepository repository;
    CustomLogoutFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CustomLogoutFilter(jwtUtil, repository);
    }

    @Test
    @DisplayName("로그아웃 시 Refresh Token을 삭제하고 200이 반환되는가?")
    void deletesExistingRefreshToken() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.get("tokenType", String.class)).thenReturn("REFRESH");
        when(jwtUtil.getClaims("refresh")).thenReturn(claims);
        when(repository.existsById("refresh")).thenReturn(true);
        MockHttpServletRequest request = logoutRequest("refresh");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).isEmpty();
        verify(repository).deleteById("refresh");
    }

    @Test
    @DisplayName("Redis에 없는 Refresh Token이면 401이 반환되고 삭제하지 않는가?")
    void returnsUnauthorizedWhenRefreshTokenNotStored() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.get("tokenType", String.class)).thenReturn("REFRESH");
        when(jwtUtil.getClaims("refresh")).thenReturn(claims);
        when(repository.existsById("refresh")).thenReturn(false);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(logoutRequest("refresh"), response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).isEmpty();
        verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("만료된 Refresh Token이라도 Redis에서 삭제하고 200이 반환되는가?")
    void deletesExpiredRefreshToken() throws Exception {
        ExpiredJwtException expired = mock(ExpiredJwtException.class);
        when(jwtUtil.getClaims("expired")).thenThrow(expired);
        when(repository.existsById("expired")).thenReturn(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(logoutRequest("expired"), response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(200);
        verify(repository).deleteById("expired");
    }

    @Test
    @DisplayName("잘못된 로그아웃 요청을 처리하고, 다른 요청은 통과시키는가?")
    void rejectsMissingMalformedOrWrongTypeTokenAndPassesOtherRequests() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(logoutRequest(null), response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).isEmpty();
        response = new MockHttpServletResponse();
        Claims claims = mock(Claims.class);
        when(claims.get("tokenType", String.class)).thenReturn("ACCESS");
        when(jwtUtil.getClaims("access")).thenReturn(claims);
        filter.doFilter(logoutRequest("access"), response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).isEmpty();
        MockHttpServletResponse nextResponse = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/other"),
                        nextResponse, new MockFilterChain());
        assertThat(nextResponse.getStatus()).isEqualTo(200);
        assertThat(nextResponse.getContentAsString()).isEmpty();
        verifyNoInteractions(repository);
    }

    private MockHttpServletRequest logoutRequest(String refreshToken) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/logout");
        if (refreshToken != null) {
            request.setCookies(new Cookie("refreshToken", refreshToken));
        }
        return request;
    }
}
