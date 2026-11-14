package com.mes.mesaichatbot.service;

import com.mes.mesaichatbot.domain.RefreshEntity;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.repository.RefreshRepository;
import com.mes.mesaichatbot.util.JWTUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshService {
    private final RefreshRepository refreshRepository;
    private final JWTUtil jwtUtil;
    private final UserService userService;

    public String refresh(String refreshToken) {

        // Refresh 토큰 검증
        Claims claims;
        try {
            claims = jwtUtil.getClaims(refreshToken);
        } catch (Exception e) {
            log.error(String.valueOf(e));
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");

        }

        String tokenType = claims.get("tokenType", String.class);

        if (!"REFRESH".equals(tokenType)) {
            throw new IllegalArgumentException("Refresh Token이 아닙니다.");
        }

        // 화이트 리스트 확인
        boolean exists = refreshRepository.existsById(refreshToken);

        if (!exists) {
            throw new IllegalArgumentException("로그아웃되었거나 만료된 Refresh Token입니다.");
        }

        // 재발급
        String username = claims.getSubject();
        UserMember member;
        try {
            member = userService.getUserEntity(username);
        } catch (UsernameNotFoundException e) {
            // 탈퇴했거나 존재하지 않는 사용자의 토큰은 즉시 폐기
            refreshRepository.deleteById(refreshToken);
            throw new IllegalArgumentException("탈퇴했거나 존재하지 않는 사용자입니다.");
        }

        // 토큰에 담긴 권한이 아닌 DB의 현재 권한으로 재발급
        return jwtUtil.createAccessToken(username, member.getRoleCode().name());
    }

    /**
     * 해당 사용자의 모든 Refresh Token을 폐기합니다.
     *
     * @param companyId  사용자의 회사번호
     * @param employeeId 사용자의 사원번호
     */
    public void revokeAllTokens(String companyId, String employeeId) {
        String username = companyId + "_" + employeeId;
        for (RefreshEntity entity : refreshRepository.findAllByUsername(username)) {
            if (entity != null) {
                refreshRepository.delete(entity);
            }
        }
    }
}
