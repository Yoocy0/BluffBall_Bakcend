package com.project.bluffball.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * OAuth 소셜 로그인 설정.
 *
 * <p>{@code application.yml}의 {@code oauth.*} 값을 바인딩한다.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {

    /** ngrok 영구 도메인 (예: https://xxx.ngrok-free.dev) — 설정 시 game-test redirect URI를 ngrok 기반으로 구성 */
    private String ngrokBaseUrl = "";

    /** 클라이언트 redirect URI 허용 목록 */
    private List<String> allowedRedirectUris = new ArrayList<>();

    /** 카카오 OAuth 클라이언트 설정 */
    private Provider kakao = new Provider();

    /** 구글 OAuth 클라이언트 설정 */
    private Provider google = new Provider();

    /** 개별 소셜 플랫폼 OAuth 클라이언트 자격 증명 */
    @Getter
    @Setter
    public static class Provider {

        /** OAuth Client ID */
        private String clientId;

        /** OAuth Client Secret */
        private String clientSecret;
    }
}
