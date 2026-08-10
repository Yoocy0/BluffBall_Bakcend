package com.project.bluffball.domain.game.service.usecase.validator;

import com.project.bluffball.domain.game.enums.BotDifficulty;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 봇 매치 요청 값 검증.
 *
 * <p>Repository에 접근하지 않으며, 인자로 받은 원시 값만 검증한다.</p>
 */
@Component
public class BotMatchValidator {

    /**
     * 난이도 필수 여부를 검증한다.
     *
     * @param difficulty 요청 난이도
     * @throws BadRequestException 난이도가 null인 경우
     */
    public void validateDifficulty(BotDifficulty difficulty) {
        if (difficulty == null) {
            throw new BadRequestException(ErrorCode.VALIDATION_FAILED, "difficulty is required");
        }
    }

    /**
     * 사람 유저 ID 유효성을 검증한다.
     *
     * @param humanUserId JWT에서 추출한 유저 ID
     * @throws BadRequestException null이거나 0 이하인 경우
     */
    public void validateHumanUserId(Long humanUserId) {
        if (humanUserId == null || humanUserId <= 0L) {
            throw new BadRequestException(ErrorCode.INVALID_REQUEST, "humanUserId is invalid");
        }
    }
}
