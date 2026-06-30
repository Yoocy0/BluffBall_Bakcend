package com.project.bluffball.domain.auth.service;

import com.project.bluffball.domain.auth.dto.request.SocialLoginRequest;
import com.project.bluffball.domain.auth.dto.request.TokenRefreshRequest;
import com.project.bluffball.domain.auth.dto.response.LoginResponse;
import com.project.bluffball.domain.auth.dto.response.TokenRefreshResponse;
import org.springframework.stereotype.Service;

/**
 * 인증 서비스.
 *
 * <p>소셜 로그인, 토큰 재발급 플로우를 usecase 레이어에 위임하여 조립한다.
 * Entity·Repository에 직접 접근하지 않는다.</p>
 */
@Service
public class AuthService {

    /**
     * 소셜 OAuth 로그인.
     *
     * @param provider path variable — {@code kakao} | {@code google}
     */
    public LoginResponse login(String provider, SocialLoginRequest request) {
        // TODO: usecase 조립 (validator → oauth client → reader/executor → jwt)
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Refresh Token으로 Access Token을 재발급한다.
     *
     * <p>유효한 Refresh Token이면 Rotation 정책에 따라 새 Refresh Token도 함께 발급한다.</p>
     */
    public TokenRefreshResponse refresh(TokenRefreshRequest request) {
        // TODO: usecase 조립 (validator → reader → executor → jwt)
        throw new UnsupportedOperationException("Not implemented");
    }
}
