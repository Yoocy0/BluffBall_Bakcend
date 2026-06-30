package com.project.bluffball.domain.auth.infrastructure.oauth;

import com.project.bluffball.domain.auth.dto.SocialUserInfo;
import com.project.bluffball.domain.user.enums.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * provider별 OAuth 클라이언트를 선택하여 유저 정보를 조회하는 Facade.
 *
 * <p>Service는 provider enum만 전달하면 되도록 OAuth Client 선택 로직을 캡슐화한다.</p>
 */
@Component
@RequiredArgsConstructor
public class OAuthUserInfoFetcher {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final GoogleOAuthClient googleOAuthClient;

    /**
     * provider에 맞는 OAuth Client로 소셜 유저 정보를 조회한다.
     */
    public SocialUserInfo fetch(SocialProvider provider, String authorizationCode, String redirectUri) {
        OAuthUserInfoClient client = resolveClient(provider);
        return client.fetchUserInfo(authorizationCode, redirectUri);
    }

    /** provider → OAuth Client 매핑 */
    private OAuthUserInfoClient resolveClient(SocialProvider provider) {
        return switch (provider) {
            case KAKAO -> kakaoOAuthClient;
            case GOOGLE -> googleOAuthClient;
        };
    }
}
