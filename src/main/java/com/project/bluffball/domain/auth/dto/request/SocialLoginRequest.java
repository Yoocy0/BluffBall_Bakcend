package com.project.bluffball.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 소셜 OAuth 로그인 REST 요청 DTO.
 *
 * <p>클라이언트가 소셜 플랫폼 OAuth 인증 후 받은 인가 코드와
 * 해당 요청에 사용한 redirect URI를 서버에 전달한다.
 * redirect URI는 서버에 등록된 허용 주소 목록과 대조하여 검증된다.</p>
 */
public record SocialLoginRequest(

        /** 소셜 플랫폼이 발급한 Authorization Code */
        @NotBlank
        String authorizationCode,

        /** OAuth 인증 요청 시 사용한 redirect URI (허용 목록 대조용) */
        @NotBlank
        String redirectUri
) {
}
