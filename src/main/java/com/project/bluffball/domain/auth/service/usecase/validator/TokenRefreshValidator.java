package com.project.bluffball.domain.auth.service.usecase.validator;

import com.project.bluffball.domain.auth.dto.request.TokenRefreshRequest;
import com.project.bluffball.global.exception.AuthUnauthorizedException;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Refresh Token 재발급 입력값 검증 (usecase/validator 계층).
 */
@Component
public class TokenRefreshValidator {

    public void validateRequest(TokenRefreshRequest request) {
        if (request == null) {
            throw new BadRequestException(ErrorCode.REQUEST_BODY_EMPTY);
        }
        if (!StringUtils.hasText(request.refreshToken())) {
            throw new BadRequestException(ErrorCode.AUTH_REFRESH_TOKEN_EMPTY);
        }
    }

    public Long validateTokenFound(Optional<Long> userId) {
        if (userId.isEmpty()) {
            throw new AuthUnauthorizedException(ErrorCode.AUTH_TOKEN_EXPIRED);
        }
        return userId.get();
    }
}
