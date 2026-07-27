package com.project.bluffball.domain.team.service.usecase.validator;

import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ConflictException;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TeamMembershipValidator")
class TeamMembershipValidatorTest {

    private final TeamMembershipValidator validator = new TeamMembershipValidator();

    @Nested
    @DisplayName("validateMember")
    class Member {

        @Test
        @DisplayName("멤버면 통과")
        void ok() {
            assertThatCode(() -> validator.validateMember(true)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("비멤버면 Forbidden")
        void notMember() {
            assertThatThrownBy(() -> validator.validateMember(false))
                    .isInstanceOf(ForbiddenException.class)
                    .extracting(ex -> ((ForbiddenException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_NOT_MEMBER);
        }
    }

    @Nested
    @DisplayName("validateLeader")
    class Leader {

        @Test
        @DisplayName("리더면 통과")
        void ok() {
            assertThatCode(() -> validator.validateLeader(true)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("리더가 아니면 Forbidden")
        void notLeader() {
            assertThatThrownBy(() -> validator.validateLeader(false))
                    .isInstanceOf(ForbiddenException.class)
                    .extracting(ex -> ((ForbiddenException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("validateCanJoin")
    class CanJoin {

        @Test
        @DisplayName("미소속이면 통과")
        void ok() {
            assertThatCode(() -> validator.validateCanJoin(false)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("이미 소속이면 Conflict")
        void alreadyJoined() {
            assertThatThrownBy(() -> validator.validateCanJoin(true))
                    .isInstanceOf(ConflictException.class)
                    .extracting(ex -> ((ConflictException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_ALREADY_JOINED);
        }
    }

    @Nested
    @DisplayName("validateCapacity")
    class Capacity {

        @Test
        @DisplayName("정원 미만이면 통과")
        void ok() {
            assertThatCode(() -> validator.validateCapacity(49)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("정원 이상이면 Conflict")
        void full() {
            assertThatThrownBy(() -> validator.validateCapacity(50))
                    .isInstanceOf(ConflictException.class)
                    .extracting(ex -> ((ConflictException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_MEMBER_LIMIT_EXCEEDED);
        }
    }

    @Nested
    @DisplayName("validateCanLeave")
    class CanLeave {

        @Test
        @DisplayName("일반 멤버는 탈퇴 가능")
        void ok() {
            assertThatCode(() -> validator.validateCanLeave(false)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("리더는 탈퇴 불가")
        void leaderCannotLeave() {
            assertThatThrownBy(() -> validator.validateCanLeave(true))
                    .isInstanceOf(ConflictException.class)
                    .extracting(ex -> ((ConflictException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_LEADER_CANNOT_LEAVE);
        }
    }

    @Nested
    @DisplayName("validateKickTarget")
    class KickTarget {

        @Test
        @DisplayName("다른 유저면 통과")
        void ok() {
            assertThatCode(() -> validator.validateKickTarget(1L, 2L)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("자기 자신 킥은 BadRequest")
        void selfKick() {
            assertThatThrownBy(() -> validator.validateKickTarget(1L, 1L))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_LEADER_CANNOT_KICK_SELF);
        }
    }
}
