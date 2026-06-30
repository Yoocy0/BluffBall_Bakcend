package com.project.bluffball.domain.auth.service;

import com.project.bluffball.domain.auth.dto.SocialUserInfo;
import com.project.bluffball.domain.auth.dto.request.SocialLoginRequest;
import com.project.bluffball.domain.auth.dto.request.TokenRefreshRequest;
import com.project.bluffball.domain.auth.dto.response.LoginResponse;
import com.project.bluffball.domain.auth.dto.response.TokenRefreshResponse;
import com.project.bluffball.domain.auth.infrastructure.jwt.JwtTokenProvider;
import com.project.bluffball.domain.auth.infrastructure.oauth.OAuthUserInfoFetcher;
import com.project.bluffball.domain.auth.service.usecase.executor.RefreshTokenExecutor;
import com.project.bluffball.domain.auth.service.usecase.reader.RefreshTokenReader;
import com.project.bluffball.domain.auth.service.usecase.validator.AccountStatusValidator;
import com.project.bluffball.domain.auth.service.usecase.validator.SocialLoginValidator;
import com.project.bluffball.domain.auth.service.usecase.validator.TokenRefreshValidator;
import com.project.bluffball.domain.user.enums.SocialProvider;
import com.project.bluffball.domain.user.service.usecase.executor.SocialLoginExecutor;
import com.project.bluffball.domain.user.service.usecase.reader.SocialAccountReader;
import com.project.bluffball.domain.user.service.usecase.reader.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 인증 서비스.
 *
 * <p>소셜 로그인·토큰 재발급 플로우를 usecase 레이어에 위임하여 조립한다.
 * Entity·Repository에 직접 접근하지 않는다.</p>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    // ── 로그인 플로우 의존성 ─────────────────────────────────────────────────────
    private final SocialLoginValidator socialLoginValidator;
    private final OAuthUserInfoFetcher oauthUserInfoFetcher;
    private final SocialAccountReader socialAccountReader;
    private final SocialLoginExecutor socialLoginExecutor;
    private final UserReader userReader;
    private final AccountStatusValidator accountStatusValidator;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenExecutor refreshTokenExecutor;

    // ── 토큰 재발급 플로우 의존성 ───────────────────────────────────────────────
    private final TokenRefreshValidator tokenRefreshValidator;
    private final RefreshTokenReader refreshTokenReader;

    /**
     * 소셜 OAuth 로그인.
     *
     * @param provider path variable — {@code kakao} | {@code google}
     */
    public LoginResponse login(String provider, SocialLoginRequest request) {

        // 1. provider·redirect URI·인가 코드 검증
        SocialProvider socialProvider = socialLoginValidator.validateAndResolveProvider(provider, request);

        // 2. 소셜 API에서 유저 식별 정보 조회
        SocialUserInfo socialUserInfo = oauthUserInfoFetcher.fetch(
                socialProvider, request.authorizationCode(), request.redirectUri());

        // 3. 기존 소셜 계정 존재 여부 조회
        Optional<Long> existingUserId = socialAccountReader.findUserIdByProviderUser(
                socialProvider, socialUserInfo.providerUserId());

        boolean isNewUser;
        Long userId;

        if (existingUserId.isEmpty()) {
            // 4a. 신규 가입 — 닉네임 자동 생성
            userId = socialLoginExecutor.registerNewUser(socialProvider, socialUserInfo.providerUserId());
            isNewUser = true;
        } else {
            // 4b. 기존 유저 로그인
            userId = existingUserId.get();
            isNewUser = false;

            // 4c. 연동 해제 상태면 재연동
            if (!socialAccountReader.isLinked(socialProvider, socialUserInfo.providerUserId())) {
                socialLoginExecutor.relink(socialProvider, socialUserInfo.providerUserId());
            }
        }

        // 5. 정지 계정 여부 검증
        accountStatusValidator.validateActive(userReader.isActive(userId));

        // 6. Access Token + Refresh Token 발급
        return issueTokenPair(userId, isNewUser);
    }

    /**
     * Refresh Token으로 Access Token을 재발급한다.
     *
     * <p>유효한 Refresh Token이면 Rotation 정책에 따라 새 Refresh Token도 함께 발급한다.</p>
     */
    public TokenRefreshResponse refresh(TokenRefreshRequest request) {

        // 1. 요청 본문 검증
        tokenRefreshValidator.validateRequest(request);

        // 2. Refresh Token → userId 조회
        Optional<Long> userIdOpt = refreshTokenReader.findUserIdByToken(request.refreshToken());
        Long userId = tokenRefreshValidator.validateTokenFound(userIdOpt);

        // 3. 정지 계정 여부 검증
        accountStatusValidator.validateActive(userReader.isActive(userId));

        // 4. Refresh Token Rotation + Access Token 재발급
        String roleName = userReader.getRoleName(userId);
        String accessToken = jwtTokenProvider.createAccessToken(userId, roleName);
        String newRefreshToken = refreshTokenExecutor.rotate(request.refreshToken(), userId);

        return new TokenRefreshResponse(
                accessToken,
                newRefreshToken,
                jwtTokenProvider.getAccessTokenExpiresInSeconds());
    }

    /** Access Token + Refresh Token 쌍 발급 (로그인 공통) */
    private LoginResponse issueTokenPair(Long userId, boolean isNewUser) {
        String roleName = userReader.getRoleName(userId);
        String accessToken = jwtTokenProvider.createAccessToken(userId, roleName);
        String refreshToken = refreshTokenExecutor.issue(userId);

        return new LoginResponse(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpiresInSeconds(),
                isNewUser);
    }
}
