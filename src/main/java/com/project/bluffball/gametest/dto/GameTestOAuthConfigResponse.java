package com.project.bluffball.gametest.dto;

/**
 * 게임 테스트 화면 OAuth 연동용 설정 응답.
 */
public record GameTestOAuthConfigResponse(

        /** 카카오 REST API 키 (Client ID) */
        String kakaoClientId,

        /** 구글 OAuth Client ID */
        String googleClientId,

        /** 카카오 OAuth Callback redirect URI (백엔드 허용 목록과 동일해야 함) */
        String kakaoRedirectUri,

        /** 구글 OAuth Callback redirect URI (백엔드 허용 목록과 동일해야 함) */
        String googleRedirectUri,

        /** 카카오 Client ID 설정 여부 */
        boolean kakaoConfigured,

        /** 구글 Client ID 설정 여부 */
        boolean googleConfigured
) {
}
