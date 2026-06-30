package com.project.bluffball.domain.auth.service.usecase.validator;

import com.project.bluffball.domain.auth.dto.request.TokenRefreshRequest;
import com.project.bluffball.global.exception.AuthUnauthorizedException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Refresh Token 재발급 입력값 검증 (usecase/validator 계층).
 *
 * <p>Repository에 접근하지 않으며, 요청 값·조회 결과의 유무 규칙만 판단한다.</p>
 */
@Component
public class TokenRefreshValidator {

    /**
     * Refresh Token 재발급 요청 본문을 검증한다.
     */
    public void validateRequest(TokenRefreshRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("요청 본문이 비어 있습니다.");
        }
        if (!StringUtils.hasText(request.refreshToken())) {
            throw new IllegalArgumentException("Refresh Token이 비어 있습니다.");
        }
    }

    /**
     * Redis에서 조회한 userId 존재 여부를 검증한다.
     *
     * @param userId Reader가 조회한 userId (없으면 empty)
     */
    public Long validateTokenFound(Optional<Long> userId) {
        if (userId.isEmpty()) {
            throw new AuthUnauthorizedException("유효하지 않거나 만료된 Refresh Token입니다.");
        }
        return userId.get();
    }
}
