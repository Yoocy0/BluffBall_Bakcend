package com.project.bluffball.domain.team.service.usecase.validator;

import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TeamPitchCardsValidator")
class TeamPitchCardsValidatorTest {

    private final TeamPitchCardsValidator validator = new TeamPitchCardsValidator();

    @Nested
    @DisplayName("validateMembersMatchLineup")
    class MembersMatchLineup {

        @Test
        @DisplayName("멤버 집합이 같으면 통과")
        void ok() {
            assertThatCode(() -> validator.validateMembersMatchLineup(
                    List.of(3L, 1L, 2L), List.of(1L, 2L, 3L)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("선택에 중복이 있으면 BadRequest")
        void duplicateSelection() {
            assertThatThrownBy(() -> validator.validateMembersMatchLineup(
                    List.of(1L, 1L, 2L), List.of(1L, 2L, 3L)))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_PITCH_CARDS_MEMBER_MISMATCH);
        }

        @Test
        @DisplayName("로스터와 다르면 BadRequest")
        void mismatch() {
            assertThatThrownBy(() -> validator.validateMembersMatchLineup(
                    List.of(1L, 2L), List.of(1L, 2L, 3L)))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_PITCH_CARDS_MEMBER_MISMATCH);
        }
    }

    @Nested
    @DisplayName("validateHandSize")
    class HandSize {

        @Test
        @DisplayName("장수 일치하면 통과")
        void ok() {
            assertThatCode(() -> validator.validateHandSize(4, 4)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("장수 불일치면 BadRequest")
        void mismatch() {
            assertThatThrownBy(() -> validator.validateHandSize(3, 4))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_PITCH_CARDS_SIZE_INVALID);
        }
    }

    @Nested
    @DisplayName("validateDropCardInHand")
    class DropCard {

        @Test
        @DisplayName("drop 카드가 핸드에 있으면 통과")
        void ok() {
            assertThatCode(() -> validator.validateDropCardInHand(List.of(10L, 20L, 30L, 40L), 20L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("drop 카드가 없으면 BadRequest")
        void missing() {
            assertThatThrownBy(() -> validator.validateDropCardInHand(List.of(10L, 20L), 99L))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_PITCH_DROP_CARD_INVALID);
        }
    }

    @Nested
    @DisplayName("validateCardsValid")
    class CardsValid {

        @Test
        @DisplayName("유효하면 통과")
        void ok() {
            assertThatCode(() -> validator.validateCardsValid(true)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("유효하지 않으면 BadRequest")
        void invalid() {
            assertThatThrownBy(() -> validator.validateCardsValid(false))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_PITCH_CARDS_INVALID);
        }
    }
}
