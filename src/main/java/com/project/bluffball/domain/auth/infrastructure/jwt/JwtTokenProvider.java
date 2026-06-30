package com.project.bluffball.domain.auth.infrastructure.jwt;

import com.project.bluffball.global.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Access Token 발급·검증 Provider.
 *
 * <p>Redis Refresh Token과 별개로, Stateless Access Token을 HMAC-SHA로 서명한다.</p>
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String ROLE_CLAIM = "role";

    private final JwtProperties jwtProperties;

    /** HMAC 서명 키 — @PostConstruct에서 초기화 */
    private SecretKey signingKey;

    @PostConstruct
    void initSigningKey() {
        signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Access Token을 발급한다.
     *
     * @param userId 유저 ID (JWT subject)
     * @param role   권한명 (JWT claim)
     */
    public String createAccessToken(Long userId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessTokenExpirationMs());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(ROLE_CLAIM, role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /** Access Token 만료까지 남은 시간 (초) */
    public long getAccessTokenExpiresInSeconds() {
        return jwtProperties.getAccessTokenExpirationMs() / 1000;
    }

    /** JWT 서명·만료 유효성 검증 */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /** JWT subject에서 userId 추출 */
    public Long extractUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    /** JWT role claim 추출 */
    public String extractRole(String token) {
        return parseClaims(token).get(ROLE_CLAIM, String.class);
    }

    /** JWT 파싱 및 claim 반환 */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
