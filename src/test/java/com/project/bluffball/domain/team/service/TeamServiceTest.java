package com.project.bluffball.domain.team.service;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.team.dto.request.CreateTeamRequest;
import com.project.bluffball.domain.team.dto.request.DonateTeamRequest;
import com.project.bluffball.domain.team.dto.request.UpdateMemberRoleRequest;
import com.project.bluffball.domain.team.dto.request.UpdateTeamLogoRequest;
import com.project.bluffball.domain.team.dto.response.TeamMemberResponse;
import com.project.bluffball.domain.team.dto.response.TeamRecordsResponse;
import com.project.bluffball.domain.team.dto.response.TeamResponse;
import com.project.bluffball.domain.team.dto.response.TeamTreasuryResponse;
import com.project.bluffball.domain.team.enums.TeamMemberRole;
import com.project.bluffball.domain.team.service.usecase.executor.TeamCreateExecutor;
import com.project.bluffball.domain.team.service.usecase.executor.TeamLogoExecutor;
import com.project.bluffball.domain.team.service.usecase.executor.TeamMembershipExecutor;
import com.project.bluffball.domain.team.service.usecase.executor.TeamPresenceExecutor;
import com.project.bluffball.domain.team.service.usecase.executor.TeamTreasuryExecutor;
import com.project.bluffball.domain.team.service.usecase.reader.TeamMemberReader;
import com.project.bluffball.domain.team.service.usecase.reader.TeamReader;
import com.project.bluffball.domain.team.service.usecase.reader.TeamRecordReader;
import com.project.bluffball.domain.team.service.usecase.reader.TeamTreasuryReader;
import com.project.bluffball.domain.team.service.usecase.validator.TeamCreateValidator;
import com.project.bluffball.domain.team.service.usecase.validator.TeamDonateValidator;
import com.project.bluffball.domain.team.service.usecase.validator.TeamMembershipValidator;
import com.project.bluffball.domain.user.service.usecase.reader.UserReader;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamService")
class TeamServiceTest {

    @Mock private TeamReader teamReader;
    @Mock private TeamMemberReader teamMemberReader;
    @Mock private TeamTreasuryReader teamTreasuryReader;
    @Mock private TeamRecordReader teamRecordReader;
    @Mock private UserReader userReader;
    @Mock private TeamCreateExecutor teamCreateExecutor;
    @Mock private TeamMembershipExecutor teamMembershipExecutor;
    @Mock private TeamTreasuryExecutor teamTreasuryExecutor;
    @Mock private TeamLogoExecutor teamLogoExecutor;
    @Mock private TeamPresenceExecutor teamPresenceExecutor;

    private TeamService teamService;

    private static final Long USER_ID = 1L;
    private static final Long TEAM_ID = 10L;
    private static final TeamResponse TEAM = new TeamResponse(TEAM_ID, "블러프", null, USER_ID, 0L, null);

    @BeforeEach
    void setUp() {
        teamService = new TeamService(
                new TeamCreateValidator(),
                new TeamMembershipValidator(),
                new TeamDonateValidator(),
                teamReader,
                teamMemberReader,
                teamTreasuryReader,
                teamRecordReader,
                userReader,
                teamCreateExecutor,
                teamMembershipExecutor,
                teamTreasuryExecutor,
                teamLogoExecutor,
                teamPresenceExecutor
        );
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("검증 통과 시 팀을 생성한다")
        void success() {
            when(teamReader.existsByName("블러프")).thenReturn(false);
            when(teamMemberReader.existsByUserId(USER_ID)).thenReturn(false);
            when(teamCreateExecutor.create("블러프", USER_ID, null)).thenReturn(TEAM_ID);
            when(teamReader.getTeamResponse(TEAM_ID)).thenReturn(TEAM);

            TeamResponse result = teamService.create(USER_ID, new CreateTeamRequest(" 블러프 ", null));

            assertThat(result).isEqualTo(TEAM);
            verify(teamCreateExecutor).create("블러프", USER_ID, null);
        }

        @Test
        @DisplayName("이름 중복이면 Conflict")
        void duplicatedName() {
            when(teamReader.existsByName("블러프")).thenReturn(true);

            assertThatThrownBy(() -> teamService.create(USER_ID, new CreateTeamRequest("블러프", null)))
                    .isInstanceOf(ConflictException.class)
                    .extracting(ex -> ((ConflictException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_NAME_DUPLICATED);
            verify(teamCreateExecutor, never()).create(anyString(), anyLong(), isNull());
        }

        @Test
        @DisplayName("이미 소속이면 Conflict")
        void alreadyJoined() {
            when(teamReader.existsByName("블러프")).thenReturn(false);
            when(teamMemberReader.existsByUserId(USER_ID)).thenReturn(true);

            assertThatThrownBy(() -> teamService.create(USER_ID, new CreateTeamRequest("블러프", null)))
                    .isInstanceOf(ConflictException.class)
                    .extracting(ex -> ((ConflictException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_ALREADY_JOINED);
        }
    }

    @Nested
    @DisplayName("getMyTeam")
    class GetMyTeam {

        @Test
        @DisplayName("소속 팀을 반환한다")
        void success() {
            when(teamMemberReader.findTeamIdByUserId(USER_ID)).thenReturn(Optional.of(TEAM_ID));
            when(teamReader.getTeamResponse(TEAM_ID)).thenReturn(TEAM);

            assertThat(teamService.getMyTeam(USER_ID)).isEqualTo(TEAM);
        }

        @Test
        @DisplayName("소속 없으면 NotFound")
        void notFound() {
            when(teamMemberReader.findTeamIdByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> teamService.getMyTeam(USER_ID))
                    .isInstanceOf(NotFoundException.class)
                    .extracting(ex -> ((NotFoundException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("join / leave / kick / delete")
    class Membership {

        @Test
        @DisplayName("가입 성공")
        void joinSuccess() {
            when(teamReader.getTeamResponse(TEAM_ID)).thenReturn(TEAM);
            when(teamMemberReader.existsByUserId(USER_ID)).thenReturn(false);
            when(teamMemberReader.countMembers(TEAM_ID)).thenReturn(3L);
            when(teamMembershipExecutor.join(USER_ID, TEAM_ID)).thenReturn(TEAM_ID);

            assertThat(teamService.join(USER_ID, TEAM_ID)).isEqualTo(TEAM);
            verify(teamMembershipExecutor).join(USER_ID, TEAM_ID);
        }

        @Test
        @DisplayName("정원 초과면 Conflict")
        void joinFull() {
            when(teamReader.getTeamResponse(TEAM_ID)).thenReturn(TEAM);
            when(teamMemberReader.existsByUserId(USER_ID)).thenReturn(false);
            when(teamMemberReader.countMembers(TEAM_ID)).thenReturn(50L);

            assertThatThrownBy(() -> teamService.join(USER_ID, TEAM_ID))
                    .isInstanceOf(ConflictException.class)
                    .extracting(ex -> ((ConflictException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_MEMBER_LIMIT_EXCEEDED);
        }

        @Test
        @DisplayName("리더는 탈퇴 불가")
        void leaveAsLeader() {
            when(teamMemberReader.isMember(TEAM_ID, USER_ID)).thenReturn(true);
            when(teamMemberReader.isLeader(TEAM_ID, USER_ID)).thenReturn(true);

            assertThatThrownBy(() -> teamService.leave(USER_ID, TEAM_ID))
                    .isInstanceOf(ConflictException.class)
                    .extracting(ex -> ((ConflictException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_LEADER_CANNOT_LEAVE);
        }

        @Test
        @DisplayName("일반 멤버 탈퇴")
        void leaveSuccess() {
            when(teamMemberReader.isMember(TEAM_ID, USER_ID)).thenReturn(true);
            when(teamMemberReader.isLeader(TEAM_ID, USER_ID)).thenReturn(false);

            teamService.leave(USER_ID, TEAM_ID);

            verify(teamMembershipExecutor).leave(USER_ID, TEAM_ID);
        }

        @Test
        @DisplayName("자기 자신 킥 불가")
        void kickSelf() {
            when(teamMemberReader.isLeader(TEAM_ID, USER_ID)).thenReturn(true);

            assertThatThrownBy(() -> teamService.kick(USER_ID, TEAM_ID, USER_ID))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_LEADER_CANNOT_KICK_SELF);
        }

        @Test
        @DisplayName("킥 성공")
        void kickSuccess() {
            Long target = 2L;
            when(teamMemberReader.isLeader(TEAM_ID, USER_ID)).thenReturn(true);
            when(teamMemberReader.isMember(TEAM_ID, target)).thenReturn(true);

            teamService.kick(USER_ID, TEAM_ID, target);

            verify(teamMembershipExecutor).kick(TEAM_ID, target);
        }

        @Test
        @DisplayName("리더만 삭제 가능")
        void deleteForbidden() {
            when(teamReader.getTeamResponse(TEAM_ID)).thenReturn(TEAM);
            when(teamMemberReader.isLeader(TEAM_ID, USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> teamService.delete(USER_ID, TEAM_ID))
                    .isInstanceOf(ForbiddenException.class)
                    .extracting(ex -> ((ForbiddenException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_FORBIDDEN);
        }

        @Test
        @DisplayName("삭제 성공")
        void deleteSuccess() {
            when(teamReader.getTeamResponse(TEAM_ID)).thenReturn(TEAM);
            when(teamMemberReader.isLeader(TEAM_ID, USER_ID)).thenReturn(true);

            teamService.delete(USER_ID, TEAM_ID);

            verify(teamMembershipExecutor).delete(TEAM_ID);
        }
    }

    @Nested
    @DisplayName("updateMemberRole")
    class UpdateMemberRole {

        @Test
        @DisplayName("리더를 MEMBER로 직접 강등하면 BadRequest")
        void demoteLeaderDirectly() {
            when(teamMemberReader.isLeader(TEAM_ID, USER_ID)).thenReturn(true);
            when(teamMemberReader.isMember(TEAM_ID, USER_ID)).thenReturn(true);

            assertThatThrownBy(() -> teamService.updateMemberRole(
                    USER_ID, TEAM_ID, USER_ID, new UpdateMemberRoleRequest(TeamMemberRole.MEMBER)))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("다른 멤버를 LEADER로 승격")
        void promoteToLeader() {
            Long target = 2L;
            TeamMemberResponse member = new TeamMemberResponse(
                    target, "타겟", TeamMemberRole.LEADER, false, LocalDateTime.now());
            when(teamMemberReader.isLeader(TEAM_ID, USER_ID)).thenReturn(true);
            when(teamMemberReader.isMember(TEAM_ID, target)).thenReturn(true);
            when(teamMemberReader.isLeader(TEAM_ID, target)).thenReturn(false);
            when(teamMemberReader.getMemberResponse(TEAM_ID, target)).thenReturn(member);

            TeamMemberResponse result = teamService.updateMemberRole(
                    USER_ID, TEAM_ID, target, new UpdateMemberRoleRequest(TeamMemberRole.LEADER));

            assertThat(result).isEqualTo(member);
            verify(teamMembershipExecutor).changeRole(TEAM_ID, target, TeamMemberRole.LEADER);
        }
    }

    @Nested
    @DisplayName("donate / treasury / logo / records / heartbeat")
    class Misc {

        @Test
        @DisplayName("기부 성공")
        void donateSuccess() {
            TeamTreasuryResponse treasury = new TeamTreasuryResponse(100L, List.of());
            when(teamMemberReader.isMember(TEAM_ID, USER_ID)).thenReturn(true);
            when(userReader.getCurrency(USER_ID)).thenReturn(200L);
            when(teamTreasuryExecutor.donate(USER_ID, TEAM_ID, 50L)).thenReturn(TEAM_ID);
            when(teamTreasuryReader.getTreasuryResponse(TEAM_ID)).thenReturn(treasury);

            assertThat(teamService.donate(USER_ID, TEAM_ID, new DonateTeamRequest(50L))).isEqualTo(treasury);
        }

        @Test
        @DisplayName("재화 부족 기부 실패")
        void donateInsufficient() {
            when(teamMemberReader.isMember(TEAM_ID, USER_ID)).thenReturn(true);
            when(userReader.getCurrency(USER_ID)).thenReturn(10L);

            assertThatThrownBy(() -> teamService.donate(USER_ID, TEAM_ID, new DonateTeamRequest(50L)))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_CURRENCY_INSUFFICIENT);
        }

        @Test
        @DisplayName("로고 변경")
        void updateLogo() {
            when(teamMemberReader.isLeader(TEAM_ID, USER_ID)).thenReturn(true);
            when(teamLogoExecutor.updateLogo(TEAM_ID, "logo.png")).thenReturn(TEAM_ID);
            when(teamReader.getTeamResponse(TEAM_ID)).thenReturn(TEAM);

            assertThat(teamService.updateLogo(USER_ID, TEAM_ID, new UpdateTeamLogoRequest("logo.png")))
                    .isEqualTo(TEAM);
        }

        @Test
        @DisplayName("기록 조회")
        void getRecords() {
            TeamRecordsResponse records = new TeamRecordsResponse(List.of(), null);
            when(teamReader.getTeamResponse(TEAM_ID)).thenReturn(TEAM);
            when(teamRecordReader.getRecords(TEAM_ID, LeagueFormat.COMPACT, null, false)).thenReturn(records);

            assertThat(teamService.getRecords(TEAM_ID, LeagueFormat.COMPACT, null, false)).isEqualTo(records);
        }

        @Test
        @DisplayName("하트비트")
        void heartbeat() {
            when(teamMemberReader.isMember(TEAM_ID, USER_ID)).thenReturn(true);

            teamService.heartbeat(USER_ID, TEAM_ID);

            verify(teamPresenceExecutor).heartbeat(TEAM_ID, USER_ID);
        }

        @Test
        @DisplayName("검색·상세·멤버·금고 조회는 Reader에 위임")
        void queries() {
            when(teamReader.search("블")).thenReturn(List.of(TEAM));
            when(teamReader.getTeamResponse(TEAM_ID)).thenReturn(TEAM);
            when(teamMemberReader.getMemberResponses(TEAM_ID)).thenReturn(List.of());
            when(teamTreasuryReader.getTreasuryResponse(TEAM_ID)).thenReturn(new TeamTreasuryResponse(0L, List.of()));

            assertThat(teamService.search("블")).containsExactly(TEAM);
            assertThat(teamService.getTeam(TEAM_ID)).isEqualTo(TEAM);
            assertThat(teamService.getMembers(TEAM_ID)).isEmpty();
            assertThat(teamService.getTreasury(TEAM_ID).treasury()).isZero();
        }
    }
}
