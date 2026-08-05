package com.project.bluffball.domain.team.service;

import com.project.bluffball.domain.card.service.usecase.reader.CardReader;
import com.project.bluffball.domain.game.config.GameModeRule;
import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.team.dto.request.UpsertTeamLineupRequest;
import com.project.bluffball.domain.team.dto.request.UpsertTeamPitchCardsRequest;
import com.project.bluffball.domain.team.dto.response.TeamLineupResponse;
import com.project.bluffball.domain.team.dto.response.TeamPitchCardsResponse;
import com.project.bluffball.domain.team.dto.response.TeamResponse;
import com.project.bluffball.domain.team.service.usecase.executor.TeamLineupExecutor;
import com.project.bluffball.domain.team.service.usecase.executor.TeamPitchCardsExecutor;
import com.project.bluffball.domain.team.service.usecase.reader.TeamLineupReader;
import com.project.bluffball.domain.team.service.usecase.reader.TeamMemberReader;
import com.project.bluffball.domain.team.service.usecase.reader.TeamPitchCardsReader;
import com.project.bluffball.domain.team.service.usecase.reader.TeamReader;
import com.project.bluffball.domain.team.service.usecase.validator.TeamLineupValidator;
import com.project.bluffball.domain.team.service.usecase.validator.TeamMembershipValidator;
import com.project.bluffball.domain.team.service.usecase.validator.TeamPitchCardsValidator;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamLineupService")
class TeamLineupServiceTest {

    @Mock private TeamReader teamReader;
    @Mock private TeamMemberReader teamMemberReader;
    @Mock private TeamLineupReader teamLineupReader;
    @Mock private TeamPitchCardsReader teamPitchCardsReader;
    @Mock private CardReader cardReader;
    @Mock private TeamLineupExecutor teamLineupExecutor;
    @Mock private TeamPitchCardsExecutor teamPitchCardsExecutor;

    private TeamLineupService teamLineupService;

    private static final Long USER_ID = 1L;
    private static final Long TEAM_ID = 10L;
    private static final TeamResponse TEAM =
            new TeamResponse(TEAM_ID, "블러프", null, USER_ID, 0L, null, com.project.bluffball.domain.team.enums.TeamJoinPolicy.OPEN);

    @BeforeEach
    void setUp() {
        teamLineupService = new TeamLineupService(
                new TeamMembershipValidator(),
                new TeamLineupValidator(),
                new TeamPitchCardsValidator(),
                teamReader,
                teamMemberReader,
                teamLineupReader,
                teamPitchCardsReader,
                cardReader,
                new GameModeRule(),
                teamLineupExecutor,
                teamPitchCardsExecutor
        );
    }

    @Nested
    @DisplayName("upsertLineup")
    class UpsertLineup {

        @Test
        @DisplayName("Compact 타순 3 + 전담 투수 저장 성공")
        void success() {
            List<Long> batting = List.of(1L, 2L, 3L);
            Long pitcher = 4L;
            List<Long> matchRoster = List.of(1L, 2L, 3L, 4L);
            TeamLineupResponse response =
                    new TeamLineupResponse(TEAM_ID, LeagueFormat.COMPACT, batting, pitcher);
            when(teamReader.getTeamResponse(TEAM_ID)).thenReturn(TEAM);
            when(teamMemberReader.isLeader(TEAM_ID, USER_ID)).thenReturn(true);
            when(teamMemberReader.areAllMembers(TEAM_ID, matchRoster)).thenReturn(true);
            when(teamLineupExecutor.upsert(TEAM_ID, LeagueFormat.COMPACT, batting, pitcher))
                    .thenReturn(TEAM_ID);
            when(teamLineupReader.getLineupResponse(TEAM_ID, LeagueFormat.COMPACT)).thenReturn(response);

            TeamLineupResponse result = teamLineupService.upsertLineup(
                    USER_ID, TEAM_ID, LeagueFormat.COMPACT, new UpsertTeamLineupRequest(batting, pitcher));

            assertThat(result).isEqualTo(response);
            verify(teamLineupExecutor).upsert(TEAM_ID, LeagueFormat.COMPACT, batting, pitcher);
        }

        @Test
        @DisplayName("리더가 아니면 Forbidden")
        void notLeader() {
            when(teamReader.getTeamResponse(TEAM_ID)).thenReturn(TEAM);
            when(teamMemberReader.isLeader(TEAM_ID, USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> teamLineupService.upsertLineup(
                    USER_ID, TEAM_ID, LeagueFormat.COMPACT,
                    new UpsertTeamLineupRequest(List.of(1L, 2L, 3L), 4L)))
                    .isInstanceOf(ForbiddenException.class)
                    .extracting(ex -> ((ForbiddenException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_FORBIDDEN);
            verify(teamLineupExecutor, never()).upsert(eq(TEAM_ID), eq(LeagueFormat.COMPACT), anyList(), eq(4L));
        }

        @Test
        @DisplayName("타순 인원 수 불일치면 BadRequest")
        void invalidSize() {
            when(teamReader.getTeamResponse(TEAM_ID)).thenReturn(TEAM);
            when(teamMemberReader.isLeader(TEAM_ID, USER_ID)).thenReturn(true);

            assertThatThrownBy(() -> teamLineupService.upsertLineup(
                    USER_ID, TEAM_ID, LeagueFormat.COMPACT,
                    new UpsertTeamLineupRequest(List.of(1L, 2L), 4L)))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_LINEUP_SIZE_INVALID);
        }

        @Test
        @DisplayName("Compact에서 선발이 타순에 있으면 BadRequest")
        void pitcherInBattingOrder() {
            when(teamReader.getTeamResponse(TEAM_ID)).thenReturn(TEAM);
            when(teamMemberReader.isLeader(TEAM_ID, USER_ID)).thenReturn(true);

            assertThatThrownBy(() -> teamLineupService.upsertLineup(
                    USER_ID, TEAM_ID, LeagueFormat.COMPACT,
                    new UpsertTeamLineupRequest(List.of(1L, 2L, 3L), 1L)))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_LINEUP_STARTING_PITCHER_INVALID);
        }
    }

    @Nested
    @DisplayName("upsertPitchCards")
    class UpsertPitchCards {

        @Test
        @DisplayName("Compact 구종 사전 선택 저장 성공(4명)")
        void success() {
            List<Long> matchRoster = List.of(1L, 2L, 3L, 4L);
            List<Long> cards = List.of(10L, 20L, 30L, 40L);
            var selections = List.of(
                    new UpsertTeamPitchCardsRequest.MemberPitchCardSelection(1L, cards, 40L),
                    new UpsertTeamPitchCardsRequest.MemberPitchCardSelection(2L, cards, 40L),
                    new UpsertTeamPitchCardsRequest.MemberPitchCardSelection(3L, cards, 40L),
                    new UpsertTeamPitchCardsRequest.MemberPitchCardSelection(4L, cards, 40L)
            );
            TeamPitchCardsResponse response = new TeamPitchCardsResponse(TEAM_ID, LeagueFormat.COMPACT, List.of());

            when(teamReader.getTeamResponse(TEAM_ID)).thenReturn(TEAM);
            when(teamMemberReader.isLeader(TEAM_ID, USER_ID)).thenReturn(true);
            when(teamLineupReader.getMatchRosterUserIds(TEAM_ID, LeagueFormat.COMPACT)).thenReturn(matchRoster);
            when(cardReader.areValidPitcherHandCards(cards)).thenReturn(true);
            when(teamPitchCardsExecutor.replaceAll(TEAM_ID, LeagueFormat.COMPACT, selections))
                    .thenReturn(TEAM_ID);
            when(teamPitchCardsReader.getPitchCardsResponse(TEAM_ID, LeagueFormat.COMPACT))
                    .thenReturn(response);

            TeamPitchCardsResponse result = teamLineupService.upsertPitchCards(
                    USER_ID, TEAM_ID, LeagueFormat.COMPACT, new UpsertTeamPitchCardsRequest(selections));

            assertThat(result).isEqualTo(response);
            verify(teamPitchCardsExecutor).replaceAll(TEAM_ID, LeagueFormat.COMPACT, selections);
        }

        @Test
        @DisplayName("출전 전원과 멤버 불일치면 BadRequest")
        void memberMismatch() {
            when(teamReader.getTeamResponse(TEAM_ID)).thenReturn(TEAM);
            when(teamMemberReader.isLeader(TEAM_ID, USER_ID)).thenReturn(true);
            when(teamLineupReader.getMatchRosterUserIds(TEAM_ID, LeagueFormat.COMPACT))
                    .thenReturn(List.of(1L, 2L, 3L, 4L));

            var selections = List.of(
                    new UpsertTeamPitchCardsRequest.MemberPitchCardSelection(
                            1L, List.of(10L, 20L, 30L, 40L), 40L)
            );

            assertThatThrownBy(() -> teamLineupService.upsertPitchCards(
                    USER_ID, TEAM_ID, LeagueFormat.COMPACT, new UpsertTeamPitchCardsRequest(selections)))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_PITCH_CARDS_MEMBER_MISMATCH);
        }
    }

    @Nested
    @DisplayName("조회")
    class Get {

        @Test
        @DisplayName("로스터·구종 조회")
        void get() {
            TeamLineupResponse lineup =
                    new TeamLineupResponse(TEAM_ID, LeagueFormat.COMPACT, List.of(1L, 2L, 3L), 4L);
            TeamPitchCardsResponse pitch =
                    new TeamPitchCardsResponse(TEAM_ID, LeagueFormat.COMPACT, List.of());
            when(teamReader.getTeamResponse(TEAM_ID)).thenReturn(TEAM);
            when(teamLineupReader.getLineupResponse(TEAM_ID, LeagueFormat.COMPACT)).thenReturn(lineup);
            when(teamPitchCardsReader.getPitchCardsResponse(TEAM_ID, LeagueFormat.COMPACT)).thenReturn(pitch);

            assertThat(teamLineupService.getLineup(TEAM_ID, LeagueFormat.COMPACT)).isEqualTo(lineup);
            assertThat(teamLineupService.getPitchCards(TEAM_ID, LeagueFormat.COMPACT)).isEqualTo(pitch);
        }
    }
}
