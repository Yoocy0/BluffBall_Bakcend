package com.project.bluffball.domain.auth.infrastructure.oauth;

import com.project.bluffball.domain.auth.dto.SocialUserInfo;

/**
 * 소셜 OAuth API 연동 클라이언트 인터페이스.
 *
 * <p>인가 코드를 Access Token으로 교환한 뒤 소셜 플랫폼 유저 정보를 조회한다.</p>
 */
public interface OAuthUserInfoClient {

    /**
     * 인가 코드로 소셜 유저 식별 정보를 조회한다.
     *
     * @param authorizationCode OAuth Authorization Code
     * @param redirectUri       OAuth 요청에 사용된 redirect URI
     */
    SocialUserInfo fetchUserInfo(String authorizationCode, String redirectUri);
}
