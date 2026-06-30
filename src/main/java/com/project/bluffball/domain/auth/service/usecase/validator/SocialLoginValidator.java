package com.project.bluffball.domain.auth.service.usecase.validator;

import com.project.bluffball.domain.auth.dto.request.SocialLoginRequest;
import com.project.bluffball.domain.user.enums.SocialProvider;
import com.project.bluffball.global.config.OAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 소셜 로그인 입력값 검증 (usecase/validator 계층).
 *
 * <p>Repository에 접근하지 않으며, provider·redirect URI 등 요청 값 규칙만 판단한다.</p>
 */
@Component
@RequiredArgsConstructor
public class SocialLoginValidator {

    private final OAuthProperties oauthProperties;

    /**
     * 소셜 로그인 요청 전체를 검증하고 provider enum을 반환한다.
     *
     * @param providerPath path variable ({@code kakao} | {@code google})
     * @param request      로그인 요청 DTO
     * @return 검증 통과한 {@link SocialProvider}
     */
    public SocialProvider validateAndResolveProvider(String providerPath, SocialLoginRequest request) {
        validateRequestNotNull(request);
        validateAuthorizationCode(request.authorizationCode());
        validateRedirectUri(request.redirectUri());
        return resolveProvider(providerPath);
    }

    // ── 개별 검증 ─────────────────────────────────────────────────────────────

    private void validateRequestNotNull(SocialLoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("요청 본문이 비어 있습니다.");
        }
    }

    /** 인가 코드 공백 여부 검증 */
    private void validateAuthorizationCode(String authorizationCode) {
        if (!StringUtils.hasText(authorizationCode)) {
            throw new IllegalArgumentException("인가 코드(authorizationCode)가 비어 있습니다.");
        }
    }

    /** redirect URI가 서버 허용 목록에 포함되는지 검증 */
    private void validateRedirectUri(String redirectUri) {
        if (!StringUtils.hasText(redirectUri)) {
            throw new IllegalArgumentException("redirect URI가 비어 있습니다.");
        }
        if (!oauthProperties.getAllowedRedirectUris().contains(redirectUri)) {
            throw new IllegalArgumentException("허용되지 않은 redirect URI입니다. redirectUri=" + redirectUri);
        }
    }

    /** path variable provider 문자열을 enum으로 변환 */
    private SocialProvider resolveProvider(String providerPath) {
        if (!StringUtils.hasText(providerPath)) {
            throw new IllegalArgumentException("provider가 비어 있습니다.");
        }
        return switch (providerPath.toLowerCase()) {
            case "kakao" -> SocialProvider.KAKAO;
            case "google" -> SocialProvider.GOOGLE;
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 provider입니다. provider=" + providerPath);
        };
    }
}
