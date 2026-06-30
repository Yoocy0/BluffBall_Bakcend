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
 * 구글 OAuth API 연동 클라이언트.
 *
 * <p>인가 코드 → Access Token → 유저 정보({@code sub}) 조회.</p>
 */
@Component
@RequiredArgsConstructor
public class GoogleOAuthClient implements OAuthUserInfoClient {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final RestClient restClient;
    private final OAuthProperties oauthProperties;

    @Override
    public SocialUserInfo fetchUserInfo(String authorizationCode, String redirectUri) {
        try {
            String accessToken = requestAccessToken(authorizationCode, redirectUri);
            return requestUserInfo(accessToken);
        } catch (RestClientResponseException ex) {
            throw new OAuthApiException("구글 OAuth API 호출에 실패했습니다.", ex);
        } catch (OAuthApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new OAuthApiException("구글 OAuth 응답 처리 중 오류가 발생했습니다.", ex);
        }
    }

    /** 인가 코드를 Access Token으로 교환 */
    private String requestAccessToken(String authorizationCode, String redirectUri) {
        OAuthProperties.Provider google = oauthProperties.getGoogle();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", google.getClientId());
        body.add("client_secret", google.getClientSecret());
        body.add("redirect_uri", redirectUri);
        body.add("code", authorizationCode);

        JsonNode response = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        return extractAccessToken(response, "구글");
    }

    /** Access Token으로 구글 유저 ID(sub) 조회 */
    private SocialUserInfo requestUserInfo(String accessToken) {
        JsonNode response = restClient.get()
                .uri(USER_INFO_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.hasNonNull("sub")) {
            throw new OAuthApiException("구글 유저 정보 응답에 sub가 없습니다.");
        }

        return new SocialUserInfo(response.get("sub").asText());
    }

    /** OAuth Token 응답에서 access_token 추출 */
    private String extractAccessToken(JsonNode response, String providerLabel) {
        if (response == null || !response.hasNonNull("access_token")) {
            throw new OAuthApiException(providerLabel + " Token 응답에 access_token이 없습니다.");
        }
        return response.get("access_token").asText();
    }
}
