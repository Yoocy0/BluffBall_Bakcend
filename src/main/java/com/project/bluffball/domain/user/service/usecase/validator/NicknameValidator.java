package com.project.bluffball.domain.user.service.usecase.validator;

import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.util.DisplayWidth;
import org.springframework.stereotype.Component;

/**
 * 닉네임 규칙 검증 (usecase/validator 계층).
 *
 * <p>표시 폭 상한은 한글 8자({@link DisplayWidth#MAX_DISPLAY_NAME_WIDTH}).</p>
 */
@Component
public class NicknameValidator {

    /**
     * 닉네임 규칙을 검증한다.
     *
     * @param nickname 닉네임
     * @throws BadRequestException 비어 있거나 표시 폭 초과 시
     */
    public void validate(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new BadRequestException(ErrorCode.NICKNAME_INVALID, "nickname is blank");
        }
        int width = DisplayWidth.of(nickname);
        if (width > DisplayWidth.MAX_DISPLAY_NAME_WIDTH) {
            throw new BadRequestException(
                    ErrorCode.NICKNAME_INVALID,
                    "displayWidth=" + width + ", max=" + DisplayWidth.MAX_DISPLAY_NAME_WIDTH);
        }
    }
}
