package com.project.bluffball.domain.league.service.usecase.validator;

import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ConflictException;
import com.project.bluffball.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LeagueProgressValidator")
class LeagueProgressValidatorTest {

    private final LeagueProgressValidator validator = new LeagueProgressValidator();

    @Nested
    @DisplayName("진입")
    class Enter {

        @Test
        @DisplayName("미진입이면 통과")
        void notEntered() {
            assertThatCode(() -> validator.validateNotAlreadyEntered(false)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("이미 진입이면 Conflict")
        void alreadyEntered() {
            assertThatThrownBy(() -> validator.validateNotAlreadyEntered(true))
                    .isInstanceOf(ConflictException.class)
                    .extracting(ex -> ((ConflictException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.LEAGUE_ALREADY_JOINED);
        }

        @Test
        @DisplayName("최초 진입은 아마4만 허용")
        void initialTier() {
            assertThatCode(() -> validator.validateInitialTier(LeagueTier.AMATEUR_4))
                    .doesNotThrowAnyException();
            assertThatThrownBy(() -> validator.validateInitialTier(LeagueTier.AMATEUR_3))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.LEAGUE_TIER_ENTRY_INVALID);
        }
    }

    @Nested
    @DisplayName("승급")
    class Promote {

        @Test
        @DisplayName("자격·다음 티어가 맞으면 통과")
        void ok() {
            assertThatCode(() -> validator.validatePromote(true, LeagueTier.AMATEUR_4, LeagueTier.AMATEUR_3))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("목표 티어가 다음이 아니면 BadRequest")
        void wrongTarget() {
            assertThatThrownBy(() -> validator.validatePromote(true, LeagueTier.AMATEUR_4, LeagueTier.AMATEUR_2))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.LEAGUE_TIER_ENTRY_INVALID);
        }

        @Test
        @DisplayName("상한 미달이면 BadRequest")
        void notReady() {
            assertThatThrownBy(() -> validator.validatePromote(false, LeagueTier.AMATEUR_4, LeagueTier.AMATEUR_3))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.LEAGUE_PROMOTE_NOT_READY);
        }
    }

    @Nested
    @DisplayName("매칭·참가비·인원")
    class Misc {

        @Test
        @DisplayName("매칭 티어 불일치")
        void tierMismatch() {
            assertThatThrownBy(() -> validator.validateMatchingTier(false))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.LEAGUE_MATCH_TIER_MISMATCH);
        }

        @Test
        @DisplayName("금고 부족")
        void treasury() {
            assertThatThrownBy(() -> validator.validateSufficientTreasury(100L, 101L))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.LEAGUE_TREASURY_INSUFFICIENT);
        }

        @Test
        @DisplayName("최소 인원 부족")
        void members() {
            assertThatThrownBy(() -> validator.validateMemberCount(2, 3))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.LEAGUE_MEMBERS_INSUFFICIENT);
        }
    }
}
