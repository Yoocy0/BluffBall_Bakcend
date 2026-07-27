package com.project.bluffball.domain.user.service.usecase.validator;

import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NicknameValidator")
class NicknameValidatorTest {

    private final NicknameValidator validator = new NicknameValidator();

    @Test
    @DisplayName("한글 8자까지 통과")
    void hangulOk() {
        assertThatCode(() -> validator.validate("가나다라마바사아")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("한글 9자는 BadRequest")
    void hangulTooLong() {
        assertThatThrownBy(() -> validator.validate("가나다라마바사아자"))
                .isInstanceOf(BadRequestException.class)
                .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NICKNAME_INVALID);
    }

    @Test
    @DisplayName("영문 16자까지 통과")
    void asciiOk() {
        assertThatCode(() -> validator.validate("a".repeat(16))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("빈 닉네임은 BadRequest")
    void blank() {
        assertThatThrownBy(() -> validator.validate(" "))
                .isInstanceOf(BadRequestException.class)
                .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NICKNAME_INVALID);
    }
}
