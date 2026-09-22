package com.mes.mesaichatbot.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;


@Component
public class JWTUtil {
    private final SecretKey key;
    private final long accessTokenExpireTime;
    private final long refreshTokenExpireTime;

    // yml파일안의 secretKey, accessToken 만료시간, refreshToken 만료시간
    public JWTUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expire-time}") long accessTokenExpireTime,
            @Value("${jwt.refresh-token-expire-time}") long refreshTokenExpireTime
    ) {
        //비밀키를 UTF-8 바이트 배열로 변환 후 토큰 생성 및 검증 때 사용할 암호화 객체로 생성
        this.key = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
        this.accessTokenExpireTime = accessTokenExpireTime;
        this.refreshTokenExpireTime = refreshTokenExpireTime;
    }

    // AccessToken 생성 메소드
    public String createAccessToken(String username, String role) {
        Date now = new Date();
        //현재 날짜시간 + accessToken만료일
        Date expireDate = new Date(now.getTime() + accessTokenExpireTime);

        //accessToken안의 내용 jwt형태로 조립
        return Jwts.builder()
                .subject(username) // 토큰 주인 식별자(예: "1_2000")
                .claim("role", role) // 사용자 권한
                .claim("tokenType", "ACCESS") //토큰 종류 구분표
                .issuedAt(now) //발급 시간
                .expiration(expireDate) //만료 시간
                .signWith(key) //비밀키(해당 키 공개시 유저 보안 뚫림)
                .compact(); // 위 내용 압축 후 jwt문자열로 변환
    }

    // RefreshToken 생성 메소드
    public String createRefreshToken(String username, String role) {
        Date now = new Date();
        //현재 날짜시간 + refreshToken만료일
        Date expireDate = new Date(now.getTime() + refreshTokenExpireTime);

        //refreshToken안의 내용 jwt형태로 조립
        return Jwts.builder()
                .subject(username) // 토큰 주인 식별자(예: "1_2000")
                .claim("role", role) // 사용자 권한
                .claim("tokenType", "REFRESH") // 토큰 종류 구분표
                .issuedAt(now) // 발급시간
                .expiration(expireDate) // 만료 시간
                .signWith(key) //비밀키(해당 키 공개시 유저 보안 뚫림)
                .compact(); // 위 내용 압축 후 jwt문자열로 변환
    }

    /**
     * 프론트엔드가 전달한 JWT 토큰을 해독하고 검증하여 내부 데이터(Claims)를 추출진행
     *
     * @param token 검사할 JWT 문자열 (Bearer 제외)
     * @return 토큰 안에 들어있는 Payload (subject, role, tokenType 등)
     * @throws io.jsonwebtoken.ExpiredJwtException 토큰의 수명이 만료되었을 때 발생
     * @throws io.jsonwebtoken.JwtException        서명이 일치하지 않거나 훼손된(위조된) 토큰일 때 발생
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key) //서버가 가진 비밀키로 위조 여부 철저히 검증
                .build()
                .parseSignedClaims(token)//실제 보안 검사 핵심 코드(서명 위조, 토큰 기한 만료 검사)
                .getPayload(); // 검증 통과 시 내부 데이터 반환
    }
}
