package com.project.bluffball.domain.league.service;

import com.project.bluffball.domain.league.dto.response.LeagueResponse;
import com.project.bluffball.domain.league.dto.response.TeamLeagueProgressResponse;
import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.domain.league.service.usecase.executor.TeamLeagueProgressExecutor;
import com.project.bluffball.domain.league.service.usecase.reader.LeagueReader;
import com.project.bluffball.domain.league.service.usecase.reader.TeamLeagueProgressReader;
import com.project.bluffball.domain.league.service.usecase.validator.LeagueProgressValidator;
import com.project.bluffball.domain.team.service.usecase.reader.TeamMemberReader;
import com.project.bluffball.domain.team.service.usecase.reader.TeamReader;
import com.project.bluffball.domain.team.service.usecase.validator.TeamMembershipValidator;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ConflictException;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.ForbiddenException;
import com.project.bluffball.global.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeagueProgressService")
class LeagueProgressServiceTest {

    @Mock private TeamLeagueProgressReader teamLeagueProgressReader;
    @Mock private LeagueReader leagueReader;
    @Mock private TeamMemberReader teamMemberReader;
    @Mock private TeamReader teamReader;
    @Mock private TeamLeagueProgressExecutor teamLeagueProgressExecutor;

    private LeagueProgressService service;

    private static final Long USER_ID = 1L;
    private static final Long TEAM_ID = 10L;
    private static final Long LEAGUE_ID = 100L;

    private static final LeagueResponse AMATEUR4_LEAGUE =
            new LeagueResponse(LEAGUE_ID, LeagueFormat.COMPACT, LeagueTier.AMATEUR_4, "컴팩트 리그 아마 4부", 1000L, 0L);

    private static final TeamLeagueProgressResponse PROGRESS =
            new TeamLeagueProgressResponse(
                    TEAM_ID, LeagueFormat.COMPACT, LeagueTier.AMATEUR_4,
                    0, 0, 150, false, LeagueTier.AMATEUR_3, 0, 0, 0);

    @BeforeEach
    void setUp() {
        service = new LeagueProgressService(
                new LeagueProgressValidator(),
                new TeamMembershipValidator(),
                teamLeagueProgressReader,
                leagueReader,
                teamMemberReader,
                teamReader,
                teamLeagueProgressExecutor
        );
    }

    @Nested
    @DisplayName("enter")
    class Enter {

        @Test
        @DisplayName("리더·인원·금고 충족 시 진입")
        void success() {
            when(teamMemberReader.findTeamIdByUserId(USER_ID)).thenReturn(Optional.of(TEAM_ID));
            when(teamMemberReader.isLeader(TEAM_ID, USER_ID)).thenReturn(true);
            when(teamLeagueProgressReader.exists(TEAM_ID, LeagueFormat.COMPACT)).thenReturn(false);
            when(leagueReader.getByFormatAndTier(LeagueFormat.COMPACT, LeagueTier.AMATEUR_4))
                    .thenReturn(AMATEUR4_LEAGUE);
            when(leagueReader.getMinTeamMembers(LEAGUE_ID)).thenReturn(3);
            when(teamMemberReader.countMembers(TEAM_ID)).thenReturn(3L);
            when(leagueReader.getEntryFee(LEAGUE_ID)).thenReturn(1000L);
            when(teamReader.getTreasury(TEAM_ID)).thenReturn(1000L);
            when(teamLeagueProgressReader.getProgressResponse(TEAM_ID, LeagueFormat.COMPACT))
                    .thenReturn(PROGRESS);

            TeamLeagueProgressResponse result = service.enter(USER_ID, LeagueFormat.COMPACT);

            assertThat(result).isEqualTo(PROGRESS);
            verify(teamLeagueProgressExecutor).enterLowestTier(TEAM_ID, LeagueFormat.COMPACT);
        }

        @Test
        @DisplayName("리더가 아니면 Forbidden")
        void notLeader() {
            when(teamMemberReader.findTeamIdByUserId(USER_ID)).thenReturn(Optional.of(TEAM_ID));
            when(teamMemberReader.isLeader(TEAM_ID, USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.enter(USER_ID, LeagueFormat.COMPACT))
                    .isInstanceOf(ForbiddenException.class)
                    .extracting(ex -> ((ForbiddenException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_FORBIDDEN);
            verify(teamLeagueProgressExecutor, never()).enterLowestTier(TEAM_ID, LeagueFormat.COMPACT);
        }

        @Test
        @DisplayName("이미 진입이면 Conflict")
        void alreadyEntered() {
            when(teamMemberReader.findTeamIdByUserId(USER_ID)).thenReturn(Optional.of(TEAM_ID));
            when(teamMemberReader.isLeader(TEAM_ID, USER_ID)).thenReturn(true);
            when(teamLeagueProgressReader.exists(TEAM_ID, LeagueFormat.COMPACT)).thenReturn(true);

            assertThatThrownBy(() -> service.enter(USER_ID, LeagueFormat.COMPACT))
                    .isInstanceOf(ConflictException.class)
                    .extracting(ex -> ((ConflictException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.LEAGUE_ALREADY_JOINED);
        }

        @Test
        @DisplayName("팀 없으면 NotFound")
        void noTeam() {
            when(teamMemberReader.findTeamIdByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.enter(USER_ID, LeagueFormat.COMPACT))
                    .isInstanceOf(NotFoundException.class)
                    .extracting(ex -> ((NotFoundException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("promote")
    class Promote {

        @Test
        @DisplayName("자격 있으면 승급")
        void success() {
            TeamLeagueProgressResponse ready = new TeamLeagueProgressResponse(
                    TEAM_ID, LeagueFormat.COMPACT, LeagueTier.AMATEUR_4,
                    150, 0, 150, true, LeagueTier.AMATEUR_3, 10, 0, 20);
            TeamLeagueProgressResponse after = new TeamLeagueProgressResponse(
                    TEAM_ID, LeagueFormat.COMPACT, LeagueTier.AMATEUR_3,
                    150, 150, 300, false, LeagueTier.AMATEUR_2, 10, 0, 20);
            LeagueResponse amateur3 = new LeagueResponse(
                    101L, LeagueFormat.COMPACT, LeagueTier.AMATEUR_3, "아마3", 2000L, 0L);

            when(teamMemberReader.findTeamIdByUserId(USER_ID)).thenReturn(Optional.of(TEAM_ID));
            when(teamMemberReader.isLeader(TEAM_ID, USER_ID)).thenReturn(true);
            when(teamLeagueProgressReader.getProgressResponse(TEAM_ID, LeagueFormat.COMPACT))
                    .thenReturn(ready, after);
            when(leagueReader.getByFormatAndTier(LeagueFormat.COMPACT, LeagueTier.AMATEUR_3))
                    .thenReturn(amateur3);
            when(leagueReader.getMinTeamMembers(101L)).thenReturn(3);
            when(teamMemberReader.countMembers(TEAM_ID)).thenReturn(3L);
            when(leagueReader.getEntryFee(101L)).thenReturn(2000L);
            when(teamReader.getTreasury(TEAM_ID)).thenReturn(5000L);

            TeamLeagueProgressResponse result =
                    service.promote(USER_ID, LeagueFormat.COMPACT, LeagueTier.AMATEUR_3);

            assertThat(result.currentTier()).isEqualTo(LeagueTier.AMATEUR_3);
            verify(teamLeagueProgressExecutor).promote(TEAM_ID, LeagueFormat.COMPACT, LeagueTier.AMATEUR_3);
        }

        @Test
        @DisplayName("자격 없으면 BadRequest")
        void notReady() {
            when(teamMemberReader.findTeamIdByUserId(USER_ID)).thenReturn(Optional.of(TEAM_ID));
            when(teamMemberReader.isLeader(TEAM_ID, USER_ID)).thenReturn(true);
            when(teamLeagueProgressReader.getProgressResponse(TEAM_ID, LeagueFormat.COMPACT))
                    .thenReturn(PROGRESS);

            assertThatThrownBy(() -> service.promote(USER_ID, LeagueFormat.COMPACT, LeagueTier.AMATEUR_3))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.LEAGUE_PROMOTE_NOT_READY);
        }
    }

    @Nested
    @DisplayName("조회")
    class Query {

        @Test
        @DisplayName("내 진행·포맷별 조회")
        void get() {
            when(teamMemberReader.findTeamIdByUserId(USER_ID)).thenReturn(Optional.of(TEAM_ID));
            when(teamLeagueProgressReader.getProgressResponses(TEAM_ID)).thenReturn(List.of(PROGRESS));
            when(teamLeagueProgressReader.getProgressResponse(TEAM_ID, LeagueFormat.COMPACT))
                    .thenReturn(PROGRESS);

            assertThat(service.getMyProgress(USER_ID)).containsExactly(PROGRESS);
            assertThat(service.getProgress(USER_ID, LeagueFormat.COMPACT)).isEqualTo(PROGRESS);
        }
    }
}
