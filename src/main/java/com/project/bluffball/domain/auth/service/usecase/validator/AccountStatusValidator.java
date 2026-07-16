package com.project.bluffball.domain.auth.service.usecase.validator;

import com.project.bluffball.global.exception.AuthForbiddenException;
import com.project.bluffball.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 계정 상태 검증 (usecase/validator 계층).
 *
 * <p>Reader가 조회한 활성 여부 등의 값을 받아 규칙 위반 여부만 판단한다.</p>
 */
@Component
public class AccountStatusValidator {

    /**
     * 계정 활성 여부를 검증한다.
     *
     * @param isActive {@link com.project.bluffball.domain.user.service.usecase.reader.UserReader#isActive} 조회 결과
     */
    public void validateActive(boolean isActive) {
        if (!isActive) {
            throw new AuthForbiddenException(ErrorCode.AUTH_ACCOUNT_SUSPENDED);
        }
    }
}
