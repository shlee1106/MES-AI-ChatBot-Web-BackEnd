package com.mes.mesaichatbot.service;

import com.mes.mesaichatbot.domain.Company;
import com.mes.mesaichatbot.domain.RefreshEntity;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.domain.UserRole;
import com.mes.mesaichatbot.repository.RefreshRepository;
import com.mes.mesaichatbot.util.JWTUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("RefreshService")
@ExtendWith(MockitoExtension.class)
class RefreshServiceTest {
    private final Company company = new Company(1L, "MES", null);

    @Mock
    RefreshRepository refreshRepository;
    @Mock
    JWTUtil jwtUtil;
    @Mock
    UserService userService;
    @InjectMocks
    RefreshService service;

    /**
     * 유효한 Refresh Token 검증 과정(서명, 타입, 화이트리스트)에 필요한 스텁을 등록합니다.
     */
    private Claims validRefreshClaims() {
        Claims claims = mock(Claims.class);
        when(jwtUtil.getClaims("refresh")).thenReturn(claims);
        when(claims.get("tokenType", String.class)).thenReturn("REFRESH");
        when(refreshRepository.existsById("refresh")).thenReturn(true);
        return claims;
    }

    @Test
    @DisplayName("재발급 시 토큰 속 권한이 아닌 DB의 현재 권한으로 Access Token을 발급하는가?")
    void reissuesWithDbRoleNotTokenRole() {
        Claims claims = validRefreshClaims();
        when(claims.getSubject()).thenReturn("1_2000");
        // 토큰에는 최고관리자 권한이 담겨 있지만 재발급에 사용되지 않아야 함
        lenient().when(claims.get("role", String.class)).thenReturn("ROLE_MASTER");
        UserMember member = new UserMember(2000L, company, "이름", "encoded", "a@example.com",
                                           UserRole.ROLE_USER, LocalDateTime.now());
        when(userService.getUserEntity("1_2000")).thenReturn(member);
        when(jwtUtil.createAccessToken("1_2000", "ROLE_USER")).thenReturn("new-access");

        assertEquals("new-access", service.refresh("refresh"));
        verify(jwtUtil).createAccessToken("1_2000", "ROLE_USER");
    }

    @Test
    @DisplayName("탈퇴했거나 존재하지 않는 사용자면 토큰을 폐기하고 예외가 발생하는가?")
    void deletesTokenAndThrowsWhenUserNotFound() {
        Claims claims = validRefreshClaims();
        when(claims.getSubject()).thenReturn("1_2000");
        when(userService.getUserEntity("1_2000")).thenThrow(new UsernameNotFoundException("missing"));

        assertThrows(IllegalArgumentException.class, () -> service.refresh("refresh"));
        verify(refreshRepository).deleteById("refresh");
        verify(jwtUtil, never()).createAccessToken(any(), any());
    }

    @Test
    @DisplayName("화이트리스트에 없는 Refresh Token이면 예외가 발생하는가?")
    void throwsWhenTokenNotInWhitelist() {
        Claims claims = mock(Claims.class);
        when(jwtUtil.getClaims("refresh")).thenReturn(claims);
        when(claims.get("tokenType", String.class)).thenReturn("REFRESH");
        when(refreshRepository.existsById("refresh")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.refresh("refresh"));
        verify(userService, never()).getUserEntity(any());
    }

    @Test
    @DisplayName("사용자의 모든 Refresh Token을 폐기하는가?")
    void revokesAllTokensOfUser() {
        RefreshEntity first = new RefreshEntity();
        first.setUsername("1_2000");
        first.setRefresh("r1");
        RefreshEntity second = new RefreshEntity();
        second.setUsername("1_2000");
        second.setRefresh("r2");
        when(refreshRepository.findAllByUsername("1_2000")).thenReturn(Arrays.asList(first, second, null));

        service.revokeAllTokens("1", "2000");

        verify(refreshRepository).delete(first);
        verify(refreshRepository).delete(second);
        verify(refreshRepository, times(2)).delete(any());
    }
}