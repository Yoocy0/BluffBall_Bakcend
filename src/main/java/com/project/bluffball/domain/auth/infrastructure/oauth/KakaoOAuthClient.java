package com.project.bluffball.domain.auth.infrastructure.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.bluffball.domain.auth.dto.SocialUserInfo;
import com.project.bluffball.global.config.OAuthProperties;
import com.project.bluffball.global.exception.OAuthApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * 카카오 OAuth API 연동 클라이언트.
 *
 * <p>인가 코드 → Access Token → 유저 정보({@code id}) 조회.</p>
 */
@Component
@RequiredArgsConstructor
public class KakaoOAuthClient implements OAuthUserInfoClient {

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient;
    private final OAuthProperties oauthProperties;

    @Override
    public SocialUserInfo fetchUserInfo(String authorizationCode, String redirectUri) {
        try {
            String accessToken = requestAccessToken(authorizationCode, redirectUri);
            return requestUserInfo(accessToken);
        } catch (RestClientResponseException ex) {
            throw new OAuthApiException("카카오 OAuth API 호출에 실패했습니다.", ex);
        } catch (OAuthApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new OAuthApiException("카카오 OAuth 응답 처리 중 오류가 발생했습니다.", ex);
        }
    }

    /** 인가 코드를 Access Token으로 교환 */
    private String requestAccessToken(String authorizationCode, String redirectUri) {
        OAuthProperties.Provider kakao = oauthProperties.getKakao();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", kakao.getClientId());
        body.add("client_secret", kakao.getClientSecret());
        body.add("redirect_uri", redirectUri);
        body.add("code", authorizationCode);

        JsonNode response = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        return extractAccessToken(response, "카카오");
    }

    /** Access Token으로 카카오 유저 ID 조회 */
    private SocialUserInfo requestUserInfo(String accessToken) {
        JsonNode response = restClient.get()
                .uri(USER_INFO_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.hasNonNull("id")) {
            throw new OAuthApiException("카카오 유저 정보 응답에 id가 없습니다.");
        }

        return new SocialUserInfo(String.valueOf(response.get("id").asLong()));
    }

    /** OAuth Token 응답에서 access_token 추출 */
    private String extractAccessToken(JsonNode response, String providerLabel) {
        if (response == null || !response.hasNonNull("access_token")) {
            throw new OAuthApiException(providerLabel + " Token 응답에 access_token이 없습니다.");
        }
        return response.get("access_token").asText();
    }
}
