package com.project.bluffball.domain.team.service.usecase.validator;

import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TeamLineupValidator")
class TeamLineupValidatorTest {

    private final TeamLineupValidator validator = new TeamLineupValidator();

    @Nested
    @DisplayName("validateSize")
    class Size {

        @Test
        @DisplayName("인원 수가 일치하면 통과")
        void ok() {
            assertThatCode(() -> validator.validateSize(3, 3)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("인원 수 불일치면 BadRequest")
        void mismatch() {
            assertThatThrownBy(() -> validator.validateSize(2, 3))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_LINEUP_SIZE_INVALID);
        }
    }

    @Nested
    @DisplayName("validateNoDuplicates")
    class NoDuplicates {

        @Test
        @DisplayName("중복 없으면 통과")
        void ok() {
            assertThatCode(() -> validator.validateNoDuplicates(List.of(1L, 2L, 3L)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("중복이면 BadRequest")
        void duplicated() {
            assertThatThrownBy(() -> validator.validateNoDuplicates(List.of(1L, 2L, 1L)))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_LINEUP_DUPLICATE_MEMBER);
        }
    }

    @Nested
    @DisplayName("validateAllMembers")
    class AllMembers {

        @Test
        @DisplayName("전원 멤버면 통과")
        void ok() {
            assertThatCode(() -> validator.validateAllMembers(true)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("비멤버 포함이면 BadRequest")
        void invalid() {
            assertThatThrownBy(() -> validator.validateAllMembers(false))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_LINEUP_MEMBER_INVALID);
        }
    }

    @Nested
    @DisplayName("validateStartingPitcherInRoster")
    class StartingPitcherInRoster {

        @Test
        @DisplayName("선발이 타순에 있으면 통과")
        void ok() {
            assertThatCode(() -> validator.validateStartingPitcherInRoster(List.of(1L, 2L, 3L), 2L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("선발이 타순에 없으면 BadRequest")
        void notInRoster() {
            assertThatThrownBy(() -> validator.validateStartingPitcherInRoster(List.of(1L, 2L, 3L), 9L))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_LINEUP_STARTING_PITCHER_INVALID);
        }

        @Test
        @DisplayName("선발 null이면 BadRequest")
        void nullPitcher() {
            assertThatThrownBy(() -> validator.validateStartingPitcherInRoster(List.of(1L, 2L, 3L), null))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_LINEUP_STARTING_PITCHER_INVALID);
        }
    }

    @Nested
    @DisplayName("validateDedicatedStartingPitcher")
    class DedicatedStartingPitcher {

        @Test
        @DisplayName("선발이 타순 밖이면 통과")
        void ok() {
            assertThatCode(() -> validator.validateDedicatedStartingPitcher(List.of(1L, 2L, 3L), 4L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("선발이 타순에 있으면 BadRequest")
        void inBattingOrder() {
            assertThatThrownBy(() -> validator.validateDedicatedStartingPitcher(List.of(1L, 2L, 3L), 2L))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_LINEUP_STARTING_PITCHER_INVALID);
        }

        @Test
        @DisplayName("선발 null이면 BadRequest")
        void nullPitcher() {
            assertThatThrownBy(() -> validator.validateDedicatedStartingPitcher(List.of(1L, 2L, 3L), null))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_LINEUP_STARTING_PITCHER_INVALID);
        }
    }

    @Nested
    @DisplayName("validateStartingPitcherPresent")
    class StartingPitcherPresent {

        @Test
        @DisplayName("선발 있으면 통과")
        void ok() {
            assertThatCode(() -> validator.validateStartingPitcherPresent(true)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("선발 없으면 BadRequest")
        void missing() {
            assertThatThrownBy(() -> validator.validateStartingPitcherPresent(false))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_LINEUP_STARTING_PITCHER_INVALID);
        }
    }
}
