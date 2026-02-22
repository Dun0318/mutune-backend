package com.mutune.backend.Security.Jwt;

import com.mutune.backend.Config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtProvider {

    private final JwtProperties jwtProperties;
    private final Key key;

    public JwtProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        // Base64 decode 제거 → 문자열 그대로 사용
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    /**
     * AccessToken 생성 (기존 버전)
     * - userId, email, role 만 포함
     * - 기존 코드와의 호환성을 위해 그대로 유지
     */
    public String createAccessToken(Long userId, String email, String role,
                                    String username, String profileImage,
                                    String provider, String bio, String website) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId)) // JWT subject = userId
                .claim("email", email)              // 이메일 claim
                .claim("role", role)                // 권한 claim
                .claim("username", username)
                .claim("profileImage" , profileImage)
                .claim("provider", provider)
                .claim("website", website)
                .setIssuedAt(new Date())            // 발급 시간
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration())) // 만료 시간
                .signWith(key)                      // 서명
                .compact();
    }

    /**
     * AccessToken 생성 (확장 버전)
     * - userId, email, role 외에 username, profileImage, provider 추가
     * - 프론트에서 토큰만 디코딩해도 최소한의 사용자 정보를 확인 가능
     */
    public String createAccessToken(Long userId, String email, String role,
                                    String username, String profileImage, String provider) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))   // JWT subject = userId
                .claim("email", email)                // 이메일 claim
                .claim("role", role)                  // 권한 claim
                .claim("username", username)          // 닉네임 claim
                .claim("profileImage", profileImage)  // 프로필 이미지 claim
                .claim("provider", provider)          // 로그인 제공자 claim
                .setIssuedAt(new Date())              // 발급 시간
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration())) // 만료 시간
                .signWith(key)                        // 서명
                .compact();
    }

    /**
     * RefreshToken 생성
     * - userId만 subject로 저장
     * - AccessToken 재발급 용도
     */
    public String createRefreshToken(Long userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))   // JWT subject = userId
                .setIssuedAt(new Date())              // 발급 시간
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshExpiration())) // 만료 시간
                .signWith(key)                        // 서명
                .compact();
    }

    /**
     * 토큰 파싱
     * - 전달받은 JWT를 해석하여 Claims(body)를 반환
     * - Claims 안에는 subject(userId), email, role 등 저장된 값들이 들어있음
     */
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)   // 서명 검증용 key
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
