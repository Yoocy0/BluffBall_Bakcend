package com.project.bluffball.domain.team.service.usecase.validator;

import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TeamDonateValidator")
class TeamDonateValidatorTest {

    private final TeamDonateValidator validator = new TeamDonateValidator();

    @Nested
    @DisplayName("validateAmount")
    class Amount {

        @Test
        @DisplayName("양수면 통과")
        void ok() {
            assertThatCode(() -> validator.validateAmount(1L)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("0 이하면 BadRequest")
        void invalid() {
            assertThatThrownBy(() -> validator.validateAmount(0L))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_DONATE_AMOUNT_INVALID);
        }
    }

    @Nested
    @DisplayName("validateSufficientCurrency")
    class SufficientCurrency {

        @Test
        @DisplayName("잔액이 충분하면 통과")
        void ok() {
            assertThatCode(() -> validator.validateSufficientCurrency(100L, 100L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("잔액 부족이면 BadRequest")
        void insufficient() {
            assertThatThrownBy(() -> validator.validateSufficientCurrency(99L, 100L))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_CURRENCY_INSUFFICIENT);
        }
    }
}
