package com.project.bluffball.domain.team.service.usecase.validator;

import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ConflictException;
import com.project.bluffball.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TeamCreateValidator")
class TeamCreateValidatorTest {

    private final TeamCreateValidator validator = new TeamCreateValidator();

    @Nested
    @DisplayName("validateName")
    class Name {

        @Test
        @DisplayName("한글 8자까지 통과")
        void hangulOk() {
            assertThatCode(() -> validator.validateName("가나다라마바사아")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("빈 이름은 BadRequest")
        void blank() {
            assertThatThrownBy(() -> validator.validateName("  "))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_NAME_INVALID);
        }

        @Test
        @DisplayName("한글 9자는 BadRequest")
        void hangulTooLong() {
            assertThatThrownBy(() -> validator.validateName("가나다라마바사아자"))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_NAME_INVALID);
        }

        @Test
        @DisplayName("영문 17자는 BadRequest")
        void asciiTooLong() {
            assertThatThrownBy(() -> validator.validateName("a".repeat(17)))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_NAME_INVALID);
        }
    }

    @Nested
    @DisplayName("validateNameNotDuplicated")
    class NameDuplicated {

        @Test
        @DisplayName("중복 없으면 통과")
        void ok() {
            assertThatCode(() -> validator.validateNameNotDuplicated(false)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("중복이면 Conflict")
        void duplicated() {
            assertThatThrownBy(() -> validator.validateNameNotDuplicated(true))
                    .isInstanceOf(ConflictException.class)
                    .extracting(ex -> ((ConflictException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_NAME_DUPLICATED);
        }
    }

    @Nested
    @DisplayName("validateNotAlreadyJoined")
    class NotAlreadyJoined {

        @Test
        @DisplayName("미소속이면 통과")
        void ok() {
            assertThatCode(() -> validator.validateNotAlreadyJoined(false)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("이미 소속이면 Conflict")
        void alreadyJoined() {
            assertThatThrownBy(() -> validator.validateNotAlreadyJoined(true))
                    .isInstanceOf(ConflictException.class)
                    .extracting(ex -> ((ConflictException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_ALREADY_JOINED);
        }
    }
}
