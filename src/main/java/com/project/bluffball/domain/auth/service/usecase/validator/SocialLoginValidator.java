package com.project.bluffball.domain.auth.service.usecase.validator;

import com.project.bluffball.domain.auth.dto.request.SocialLoginRequest;
import com.project.bluffball.domain.user.enums.SocialProvider;
import com.project.bluffball.global.config.OAuthProperties;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 소셜 로그인 입력값 검증 (usecase/validator 계층).
 */
@Component
@RequiredArgsConstructor
public class SocialLoginValidator {

    private final OAuthProperties oauthProperties;

    public SocialProvider validateAndResolveProvider(String providerPath, SocialLoginRequest request) {
        validateRequestNotNull(request);
        validateAuthorizationCode(request.authorizationCode());
        validateRedirectUri(request.redirectUri());
        return resolveProvider(providerPath);
    }

    private void validateRequestNotNull(SocialLoginRequest request) {
        if (request == null) {
            throw new BadRequestException(ErrorCode.REQUEST_BODY_EMPTY);
        }
    }

    private void validateAuthorizationCode(String authorizationCode) {
        if (!StringUtils.hasText(authorizationCode)) {
            throw new BadRequestException(ErrorCode.AUTH_AUTHORIZATION_CODE_EMPTY);
        }
    }

    private void validateRedirectUri(String redirectUri) {
        if (!StringUtils.hasText(redirectUri)) {
            throw new BadRequestException(ErrorCode.AUTH_REDIRECT_URI_EMPTY);
        }
        if (!oauthProperties.getAllowedRedirectUris().contains(redirectUri)) {
            throw new BadRequestException(ErrorCode.AUTH_REDIRECT_URI_NOT_ALLOWED, "redirectUri=" + redirectUri);
        }
    }

    private SocialProvider resolveProvider(String providerPath) {
        if (!StringUtils.hasText(providerPath)) {
            throw new BadRequestException(ErrorCode.AUTH_PROVIDER_EMPTY);
        }
        return switch (providerPath.toLowerCase()) {
            case "kakao" -> SocialProvider.KAKAO;
            case "google" -> SocialProvider.GOOGLE;
            default -> throw new BadRequestException(ErrorCode.AUTH_PROVIDER_UNSUPPORTED, "provider=" + providerPath);
        };
    }
}
