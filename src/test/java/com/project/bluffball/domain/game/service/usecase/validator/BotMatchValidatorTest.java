package com.project.bluffball.domain.game.service.usecase.validator;

import com.project.bluffball.domain.game.enums.BotDifficulty;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BotMatchValidator")
class BotMatchValidatorTest {

    private final BotMatchValidator validator = new BotMatchValidator();

    @Test
    @DisplayName("난이도가 있으면 통과")
    void difficultyOk() {
        assertThatCode(() -> validator.validateDifficulty(BotDifficulty.NORMAL))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("난이도 null이면 VALIDATION_FAILED")
    void difficultyNull() {
        assertThatThrownBy(() -> validator.validateDifficulty(null))
                .isInstanceOf(BadRequestException.class)
                .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("사람 userId가 유효하면 통과")
    void humanUserIdOk() {
        assertThatCode(() -> validator.validateHumanUserId(1L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("사람 userId가 null이면 INVALID_REQUEST")
    void humanUserIdNull() {
        assertThatThrownBy(() -> validator.validateHumanUserId(null))
                .isInstanceOf(BadRequestException.class)
                .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }
}
