package com.project.bluffball.gametest.web;

import com.project.bluffball.gametest.dto.GameTestOAuthConfigResponse;
import com.project.bluffball.global.config.OAuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게임 테스트 화면용 OAuth 설정 API.
 *
 * <p>프론트 테스트 페이지가 Client ID·redirect URI를 서버 설정과 동기화하도록 제공한다.</p>
 *
 * <p>ngrok-base-url이 설정된 경우 각 provider 전용 백엔드 콜백 URI를 반환하고,
 * 미설정 시 현재 요청 호스트 기준 {@code /game-test/OAuthCallback.html}로 fallback한다.</p>
 */
@RestController
@RequestMapping("/game-test/api")
@RequiredArgsConstructor
public class GameTestOAuthConfigController {

    private static final String OAUTH_CALLBACK_PATH = "/game-test/OAuthCallback.html";
    private static final String BACKEND_CALLBACK_PATH = "/api/v1/auth/login/";

    private final OAuthProperties oauthProperties;

    /**
     * OAuth 로그인 버튼 클릭 시 필요한 Client ID·redirect URI를 반환한다.
     */
    @GetMapping("/oauth-config")
    public GameTestOAuthConfigResponse getOAuthConfig(HttpServletRequest request) {
        String kakaoClientId = oauthProperties.getKakao().getClientId();
        String googleClientId = oauthProperties.getGoogle().getClientId();

        return new GameTestOAuthConfigResponse(
                kakaoClientId,
                googleClientId,
                buildRedirectUri(request, "kakao"),
                buildRedirectUri(request, "google"),
                isConfigured(kakaoClientId),
                isConfigured(googleClientId));
    }

    /** Client ID가 비어 있지 않은지 확인 */
    private boolean isConfigured(String clientId) {
        return clientId != null && !clientId.isBlank();
    }

    /**
     * provider별 OAuth Callback URI 생성.
     *
     * <p>ngrok-base-url 설정 시: {@code {ngrokBaseUrl}/api/v1/auth/login/{provider}}<br>
     * 미설정 시: 현재 요청 호스트 기준 {@code /game-test/OAuthCallback.html}</p>
     */
    private String buildRedirectUri(HttpServletRequest request, String provider) {
        String ngrokBase = oauthProperties.getNgrokBaseUrl();
        if (ngrokBase != null && !ngrokBase.isBlank()) {
            return ngrokBase + BACKEND_CALLBACK_PATH + provider;
        }
        int port = request.getServerPort();
        boolean defaultPort = port == 80 || port == 443;
        String portPart = defaultPort ? "" : ":" + port;
        return request.getScheme() + "://" + request.getServerName() + portPart + OAUTH_CALLBACK_PATH;
    }
}
