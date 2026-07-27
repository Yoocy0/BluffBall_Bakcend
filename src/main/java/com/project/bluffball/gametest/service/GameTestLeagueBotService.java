package com.project.bluffball.gametest.service;

import com.project.bluffball.domain.game.config.GameModeRule;
import com.project.bluffball.domain.game.dto.request.LeagueMatchQueueJoinRequest;
import com.project.bluffball.domain.game.dto.response.MatchJoinResponse;
import com.project.bluffball.domain.game.service.LeagueMatchService;
import com.project.bluffball.domain.game.service.usecase.reader.PitchCardReader;
import com.project.bluffball.domain.league.config.LeagueTierRule;
import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.domain.league.service.LeagueProgressService;
import com.project.bluffball.domain.league.service.usecase.reader.LeagueReader;
import com.project.bluffball.domain.league.service.usecase.reader.TeamLeagueProgressReader;
import com.project.bluffball.domain.team.dto.request.CreateTeamRequest;
import com.project.bluffball.domain.team.dto.request.DonateTeamRequest;
import com.project.bluffball.domain.team.dto.request.UpsertTeamLineupRequest;
import com.project.bluffball.domain.team.dto.request.UpsertTeamPitchCardsRequest;
import com.project.bluffball.domain.team.dto.response.TeamLineupResponse;
import com.project.bluffball.domain.team.dto.response.TeamMemberResponse;
import com.project.bluffball.domain.team.dto.response.TeamResponse;
import com.project.bluffball.domain.team.service.TeamLineupService;
import com.project.bluffball.domain.team.service.TeamService;
import com.project.bluffball.domain.team.service.usecase.reader.TeamLineupReader;
import com.project.bluffball.domain.team.service.usecase.reader.TeamMemberReader;
import com.project.bluffball.domain.team.service.usecase.reader.TeamReader;
import com.project.bluffball.domain.team.service.usecase.validator.TeamMembershipValidator;
import com.project.bluffball.domain.user.record.enums.GameMode;
import com.project.bluffball.gametest.bot.BotRole;
import com.project.bluffball.gametest.bot.CompactRosterGapPlanner;
import com.project.bluffball.gametest.bot.GameTestBotUserExecutor;
import com.project.bluffball.gametest.dto.EnqueueOpponentBotRequest;
import com.project.bluffball.gametest.dto.EnqueueOpponentBotResponse;
import com.project.bluffball.gametest.dto.FillCompactRosterRequest;
import com.project.bluffball.gametest.dto.FillCompactRosterResponse;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 테스트용 Compact 리그 봇 로스터 빈자리 채움.
 *
 * <p>팀 인원·라인업(타자 3 + 전담 투수 1)·구종 사전선택·리그 참가를
 * 부족한 만큼만 봇으로 채운다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameTestLeagueBotService {

    private static final LeagueFormat FORMAT = LeagueFormat.COMPACT;
    private static final long TREASURY_BUFFER = 1_000L;

    private final TeamService teamService;
    private final TeamLineupService teamLineupService;
    private final LeagueProgressService leagueProgressService;
    private final TeamMemberReader teamMemberReader;
    private final TeamLineupReader teamLineupReader;
    private final TeamReader teamReader;
    private final TeamLeagueProgressReader teamLeagueProgressReader;
    private final LeagueReader leagueReader;
    private final TeamMembershipValidator teamMembershipValidator;
    private final GameTestBotUserExecutor gameTestBotUserExecutor;
    private final PitchCardReader pitchCardReader;
    private final GameModeRule gameModeRule;
    private final LeagueMatchService leagueMatchService;

    /**
     * Compact 로스터 빈자리를 봇으로 채운다.
     *
     * @param request 리더·팀·본인 역할
     * @return 채움 결과
     */
    @Transactional
    public FillCompactRosterResponse fillCompactRoster(FillCompactRosterRequest request) {
        Long leaderUserId = request.leaderUserId();
        BotRole leaderRole = request.myRole() != null ? request.myRole() : BotRole.BATTER;

        Long teamId = resolveOrCreateTeam(leaderUserId, request.teamId());
        teamMembershipValidator.validateLeader(teamMemberReader.isLeader(teamId, leaderUserId));

        List<Long> memberIds = teamService.getMembers(teamId).stream()
                .map(TeamMemberResponse::userId)
                .toList();

        List<Long> existingBatters = List.of();
        Long existingPitcher = null;
        if (teamLineupReader.exists(teamId, FORMAT)) {
            TeamLineupResponse existing = teamLineupService.getLineup(teamId, FORMAT);
            existingBatters = existing.userIds();
            existingPitcher = existing.startingPitcherUserId();
        }

        CompactRosterGapPlanner.Plan plan = CompactRosterGapPlanner.plan(
                leaderUserId, memberIds, existingBatters, existingPitcher, leaderRole);

        List<FillCompactRosterResponse.CreatedBot> created = new ArrayList<>();
        List<Long> batters = new ArrayList<>(plan.batterUserIds());
        Long pitcher = plan.pitcherUserId();

        for (int i = 0; i < plan.battersToCreate(); i++) {
            Long botId = createAndJoinBot(teamId, BotRole.BATTER);
            batters.add(botId);
            created.add(new FillCompactRosterResponse.CreatedBot(botId, BotRole.BATTER));
        }
        if (plan.pitcherToCreate() > 0) {
            Long botId = createAndJoinBot(teamId, BotRole.PITCHER);
            pitcher = botId;
            created.add(new FillCompactRosterResponse.CreatedBot(botId, BotRole.PITCHER));
        }

        if (batters.size() != CompactRosterGapPlanner.BATTING_ORDER_SIZE || pitcher == null) {
            throw new BadRequestException(
                    ErrorCode.TEAM_LINEUP_SIZE_INVALID,
                    "failed to fill compact roster batters=" + batters.size() + " pitcher=" + pitcher);
        }

        boolean alreadyInLeague = teamLeagueProgressReader.exists(teamId, FORMAT);
        ensureFunded(leaderUserId, teamId);
        boolean leagueEntered = false;
        if (!alreadyInLeague) {
            leagueProgressService.enter(leaderUserId, FORMAT);
            leagueEntered = true;
        }

        if (!plan.keepExisting()) {
            teamLineupService.upsertLineup(
                    leaderUserId,
                    teamId,
                    FORMAT,
                    new UpsertTeamLineupRequest(batters, pitcher));
        }
        upsertDefaultPitchCards(leaderUserId, teamId, batters, pitcher);

        log.info("Compact 로스터 채움 teamId={} leader={} batters={} pitcher={} createdBots={} entered={}",
                teamId, leaderUserId, batters, pitcher, created.size(), leagueEntered);

        return new FillCompactRosterResponse(
                teamId,
                leaderUserId,
                List.copyOf(batters),
                pitcher,
                List.copyOf(created),
                plan.keepExisting(),
                leagueEntered);
    }

    /**
     * 상대 Compact 봇 팀을 만들고 매칭 큐에 넣는다.
     *
     * <p>사람 팀이 같은 format/tier로 큐에 들어오면 매칭된다.
     * 반환된 {@code botUserIds}를 자동 플레이에 넘기면 된다.</p>
     *
     * @param request 티어(선택)
     * @return 봇 팀·큐 상태
     */
    @Transactional
    public EnqueueOpponentBotResponse enqueueOpponentBotTeam(EnqueueOpponentBotRequest request) {
        LeagueTier requestedTier = request != null && request.tier() != null
                ? request.tier()
                : LeagueTierRule.lowestTier();

        Long leaderUserId = gameTestBotUserExecutor.createBotUser(BotRole.PITCHER);
        FillCompactRosterResponse filled = fillCompactRoster(
                new FillCompactRosterRequest(leaderUserId, null, BotRole.PITCHER));

        LeagueTier actualTier = teamLeagueProgressReader.getCurrentTier(filled.teamId(), FORMAT);
        if (actualTier != requestedTier) {
            log.warn("봇 팀 티어가 요청과 다름 requested={} actual={} — actual로 큐 진입",
                    requestedTier, actualTier);
        }

        MatchJoinResponse join = leagueMatchService.joinQueue(
                leaderUserId,
                new LeagueMatchQueueJoinRequest(FORMAT, actualTier));

        Set<Long> botIds = new LinkedHashSet<>();
        botIds.add(leaderUserId);
        botIds.addAll(filled.batterUserIds());
        botIds.add(filled.pitcherUserId());
        for (FillCompactRosterResponse.CreatedBot bot : filled.createdBots()) {
            botIds.add(bot.userId());
        }

        log.info("상대 봇 큐 진입 teamId={} leader={} tier={} status={} matchSessionId={}",
                filled.teamId(), leaderUserId, actualTier, join.status(), join.matchSessionId());

        return new EnqueueOpponentBotResponse(
                filled.teamId(),
                leaderUserId,
                List.copyOf(botIds),
                filled.batterUserIds(),
                filled.pitcherUserId(),
                actualTier,
                join.status(),
                join.matchSessionId());
    }

    /**
     * 팀 ID를 확정한다. 없으면 리더로 창단한다.
     *
     * @param leaderUserId 리더
     * @param teamIdOrNull 요청 팀 ID
     * @return 팀 ID
     */
    private Long resolveOrCreateTeam(Long leaderUserId, Long teamIdOrNull) {
        if (teamIdOrNull != null) {
            teamReader.getTeamResponse(teamIdOrNull);
            return teamIdOrNull;
        }
        return teamMemberReader.findTeamIdByUserId(leaderUserId)
                .orElseGet(() -> {
                    String name = "BT" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
                    TeamResponse created = teamService.create(leaderUserId, new CreateTeamRequest(name, null));
                    return created.teamId();
                });
    }

    /**
     * 봇 유저를 만들고 팀에 합류시킨다.
     *
     * @param teamId 팀 ID
     * @param role 봇 역할
     * @return bot userId
     */
    private Long createAndJoinBot(Long teamId, BotRole role) {
        Long botUserId = gameTestBotUserExecutor.createBotUser(role);
        teamService.join(botUserId, teamId);
        return botUserId;
    }

    /**
     * 참가비 이상 팀 재정을 맞춘다.
     *
     * @param leaderUserId 리더
     * @param teamId 팀 ID
     */
    private void ensureFunded(Long leaderUserId, Long teamId) {
        Long leagueId = leagueReader.getByFormatAndTier(FORMAT, LeagueTierRule.lowestTier()).leagueId();
        long entryFee = leagueReader.getEntryFee(leagueId);
        long treasury = teamReader.getTreasury(teamId);
        long need = entryFee + TREASURY_BUFFER - treasury;
        if (need <= 0) {
            return;
        }
        gameTestBotUserExecutor.addCurrency(leaderUserId, need);
        teamService.donate(leaderUserId, teamId, new DonateTeamRequest(need));
    }

    /**
     * 출전 전원에 기본 구종 핸드를 넣는다.
     *
     * @param leaderUserId 리더
     * @param teamId 팀 ID
     * @param batters 타순
     * @param pitcher 투수
     */
    private void upsertDefaultPitchCards(
            Long leaderUserId,
            Long teamId,
            List<Long> batters,
            Long pitcher) {
        int handSize = gameModeRule.getHandSize(GameMode.COMPACT_LEAGUE);
        List<Long> allPitchIds = pitchCardReader.findAllIds();
        if (allPitchIds.size() < handSize) {
            throw new BadRequestException(
                    ErrorCode.GAME_CARD_POOL_INSUFFICIENT,
                    "not enough pitch cards in catalog size=" + allPitchIds.size());
        }
        List<Long> hand = allPitchIds.subList(0, handSize);
        Long dropCardId = hand.get(hand.size() - 1);

        List<Long> roster = new ArrayList<>(batters);
        if (!roster.contains(pitcher)) {
            roster.add(pitcher);
        }

        List<UpsertTeamPitchCardsRequest.MemberPitchCardSelection> selections = roster.stream()
                .map(userId -> new UpsertTeamPitchCardsRequest.MemberPitchCardSelection(
                        userId, List.copyOf(hand), dropCardId))
                .toList();

        teamLineupService.upsertPitchCards(
                leaderUserId,
                teamId,
                FORMAT,
                new UpsertTeamPitchCardsRequest(selections));
    }
}
