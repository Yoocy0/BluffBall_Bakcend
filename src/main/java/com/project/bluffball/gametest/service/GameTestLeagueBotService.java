package com.project.bluffball.gametest.service;

import com.project.bluffball.domain.card.service.UserPitchCardService;
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
import com.project.bluffball.gametest.bot.FullRosterGapPlanner;
import com.project.bluffball.gametest.bot.GameTestBotUserExecutor;
import com.project.bluffball.gametest.dto.EnqueueOpponentBotRequest;
import com.project.bluffball.gametest.dto.EnqueueOpponentBotResponse;
import com.project.bluffball.gametest.dto.FillCompactRosterRequest;
import com.project.bluffball.gametest.dto.FillCompactRosterResponse;
import com.project.bluffball.gametest.dto.FillRosterRequest;
import com.project.bluffball.gametest.dto.FillRosterResponse;
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
 * 테스트용 리그 봇 로스터 빈자리 채움.
 *
 * <p>Compact(4) / Full(9) 포맷별 부족한 인원만 봇으로 채우고,
 * 라인업·구종 사전선택·리그 참가까지 맞춘다. 이미 팀에 있는 멤버·봇은 재사용한다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameTestLeagueBotService {

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
    private final UserPitchCardService userPitchCardService;
    private final GameModeRule gameModeRule;
    private final LeagueMatchService leagueMatchService;

    /**
     * Compact 전용 래퍼 (하위 호환).
     *
     * @param request Compact 요청
     * @return Compact 응답 형태
     */
    @Transactional
    public FillCompactRosterResponse fillCompactRoster(FillCompactRosterRequest request) {
        FillRosterResponse filled = fillRoster(new FillRosterRequest(
                request.leaderUserId(),
                request.teamId(),
                LeagueFormat.COMPACT,
                request.myRole()));
        List<FillCompactRosterResponse.CreatedBot> created = filled.createdBots().stream()
                .map(b -> new FillCompactRosterResponse.CreatedBot(b.userId(), b.role()))
                .toList();
        return new FillCompactRosterResponse(
                filled.teamId(),
                filled.leaderUserId(),
                filled.batterUserIds(),
                filled.pitcherUserId(),
                created,
                filled.keptExistingLineup(),
                filled.leagueEntered());
    }

    /**
     * Compact / Full 로스터 빈자리를 봇으로 채운다.
     *
     * @param request 리더·팀·포맷·본인 역할
     * @return 채움 결과
     */
    @Transactional
    public FillRosterResponse fillRoster(FillRosterRequest request) {
        Long leaderUserId = request.leaderUserId();
        LeagueFormat format = request.format();
        BotRole leaderRole = request.myRole() != null ? request.myRole() : BotRole.BATTER;

        Long teamId = resolveOrCreateTeam(leaderUserId, request.teamId());
        teamMembershipValidator.validateLeader(teamMemberReader.isLeader(teamId, leaderUserId));

        List<Long> memberIds = teamService.getMembers(teamId).stream()
                .map(TeamMemberResponse::userId)
                .toList();

        List<Long> existingBatters = List.of();
        Long existingPitcher = null;
        if (teamLineupReader.exists(teamId, format)) {
            TeamLineupResponse existing = teamLineupService.getLineup(teamId, format);
            existingBatters = existing.userIds();
            existingPitcher = existing.startingPitcherUserId();
        }

        return switch (format) {
            case COMPACT -> fillCompact(
                    leaderUserId, teamId, memberIds, existingBatters, existingPitcher, leaderRole);
            case FULL -> fillFull(
                    leaderUserId, teamId, memberIds, existingBatters, existingPitcher, leaderRole);
        };
    }

    /**
     * Compact 빈자리 채움.
     */
    private FillRosterResponse fillCompact(
            Long leaderUserId,
            Long teamId,
            List<Long> memberIds,
            List<Long> existingBatters,
            Long existingPitcher,
            BotRole leaderRole) {

        CompactRosterGapPlanner.Plan plan = CompactRosterGapPlanner.plan(
                leaderUserId, memberIds, existingBatters, existingPitcher, leaderRole);

        List<FillRosterResponse.CreatedBot> created = new ArrayList<>();
        List<Long> batters = new ArrayList<>(plan.batterUserIds());
        Long pitcher = plan.pitcherUserId();

        for (int i = 0; i < plan.battersToCreate(); i++) {
            Long botId = createAndJoinBot(teamId, BotRole.BATTER);
            batters.add(botId);
            created.add(new FillRosterResponse.CreatedBot(botId, BotRole.BATTER));
        }
        if (plan.pitcherToCreate() > 0) {
            Long botId = createAndJoinBot(teamId, BotRole.PITCHER);
            pitcher = botId;
            created.add(new FillRosterResponse.CreatedBot(botId, BotRole.PITCHER));
        }

        if (batters.size() != CompactRosterGapPlanner.BATTING_ORDER_SIZE || pitcher == null) {
            throw new BadRequestException(
                    ErrorCode.TEAM_LINEUP_SIZE_INVALID,
                    "failed to fill compact roster batters=" + batters.size() + " pitcher=" + pitcher);
        }

        return finalizeRoster(
                LeagueFormat.COMPACT,
                leaderUserId,
                teamId,
                batters,
                pitcher,
                created,
                plan.keepExisting());
    }

    /**
     * Full 빈자리 채움.
     */
    private FillRosterResponse fillFull(
            Long leaderUserId,
            Long teamId,
            List<Long> memberIds,
            List<Long> existingBatters,
            Long existingPitcher,
            BotRole leaderRole) {

        FullRosterGapPlanner.Plan plan = FullRosterGapPlanner.plan(
                leaderUserId, memberIds, existingBatters, existingPitcher, leaderRole);

        List<FillRosterResponse.CreatedBot> created = new ArrayList<>();
        List<Long> batters = new ArrayList<>(plan.batterUserIds());

        for (int i = 0; i < plan.battersToCreate(); i++) {
            Long botId = createAndJoinBot(teamId, BotRole.BATTER);
            batters.add(botId);
            created.add(new FillRosterResponse.CreatedBot(botId, BotRole.BATTER));
        }

        Long pitcher = plan.pitcherUserId();
        if (pitcher == null) {
            pitcher = batters.get(batters.size() - 1);
        }

        if (batters.size() != FullRosterGapPlanner.BATTING_ORDER_SIZE || !batters.contains(pitcher)) {
            throw new BadRequestException(
                    ErrorCode.TEAM_LINEUP_SIZE_INVALID,
                    "failed to fill full roster batters=" + batters.size() + " pitcher=" + pitcher);
        }

        return finalizeRoster(
                LeagueFormat.FULL,
                leaderUserId,
                teamId,
                batters,
                pitcher,
                created,
                plan.keepExisting());
    }

    /**
     * 리그 진입·라인업·구종 저장 후 응답.
     */
    private FillRosterResponse finalizeRoster(
            LeagueFormat format,
            Long leaderUserId,
            Long teamId,
            List<Long> batters,
            Long pitcher,
            List<FillRosterResponse.CreatedBot> created,
            boolean keepExisting) {

        boolean alreadyInLeague = teamLeagueProgressReader.exists(teamId, format);
        ensureFunded(leaderUserId, teamId, format);
        boolean leagueEntered = false;
        if (!alreadyInLeague) {
            leagueProgressService.enter(leaderUserId, format);
            leagueEntered = true;
        }

        if (!keepExisting) {
            teamLineupService.upsertLineup(
                    leaderUserId,
                    teamId,
                    format,
                    new UpsertTeamLineupRequest(batters, pitcher));
        }
        upsertDefaultPitchCards(leaderUserId, teamId, format, batters, pitcher);

        log.info("로스터 채움 format={} teamId={} leader={} batters={} pitcher={} createdBots={} entered={}",
                format, teamId, leaderUserId, batters, pitcher, created.size(), leagueEntered);

        return new FillRosterResponse(
                teamId,
                format,
                leaderUserId,
                List.copyOf(batters),
                pitcher,
                List.copyOf(created),
                keepExisting,
                leagueEntered);
    }

    /**
     * 상대 봇 팀을 만들고 매칭 큐에 넣는다.
     *
     * @param request 포맷·티어(선택)
     * @return 봇 팀·큐 상태
     */
    @Transactional
    public EnqueueOpponentBotResponse enqueueOpponentBotTeam(EnqueueOpponentBotRequest request) {
        LeagueFormat format = request != null && request.format() != null
                ? request.format()
                : LeagueFormat.COMPACT;
        LeagueTier requestedTier = request != null && request.tier() != null
                ? request.tier()
                : LeagueTierRule.lowestTier();

        Long leaderUserId = gameTestBotUserExecutor.createBotUser(BotRole.PITCHER);
        FillRosterResponse filled = fillRoster(
                new FillRosterRequest(leaderUserId, null, format, BotRole.PITCHER));

        LeagueTier actualTier = teamLeagueProgressReader.getCurrentTier(filled.teamId(), format);
        if (actualTier != requestedTier) {
            log.warn("봇 팀 티어가 요청과 다름 requested={} actual={} — actual로 큐 진입",
                    requestedTier, actualTier);
        }

        MatchJoinResponse join = leagueMatchService.joinQueue(
                leaderUserId,
                new LeagueMatchQueueJoinRequest(format, actualTier));

        Set<Long> botIds = new LinkedHashSet<>();
        botIds.add(leaderUserId);
        botIds.addAll(filled.batterUserIds());
        botIds.add(filled.pitcherUserId());
        for (FillRosterResponse.CreatedBot bot : filled.createdBots()) {
            botIds.add(bot.userId());
        }

        log.info("상대 봇 큐 진입 format={} teamId={} leader={} tier={} status={} matchSessionId={}",
                format, filled.teamId(), leaderUserId, actualTier, join.status(), join.matchSessionId());

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
                    TeamResponse created = teamService.create(leaderUserId, new CreateTeamRequest(name, null, null));
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
     * @param format 리그 포맷
     */
    private void ensureFunded(Long leaderUserId, Long teamId, LeagueFormat format) {
        Long leagueId = leagueReader.getByFormatAndTier(format, LeagueTierRule.lowestTier()).leagueId();
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
     * @param format 리그 포맷
     * @param batters 타순
     * @param pitcher 투수
     */
    private void upsertDefaultPitchCards(
            Long leaderUserId,
            Long teamId,
            LeagueFormat format,
            List<Long> batters,
            Long pitcher) {
        GameMode gameMode = format == LeagueFormat.COMPACT
                ? GameMode.COMPACT_LEAGUE
                : GameMode.FULL_LEAGUE;
        int handSize = gameModeRule.getHandSize(gameMode);
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

        for (Long rosterUserId : roster) {
            userPitchCardService.acquireAllMissing(rosterUserId);
        }

        List<UpsertTeamPitchCardsRequest.MemberPitchCardSelection> selections = roster.stream()
                .map(userId -> new UpsertTeamPitchCardsRequest.MemberPitchCardSelection(
                        userId, List.copyOf(hand), dropCardId))
                .toList();

        teamLineupService.upsertPitchCards(
                leaderUserId,
                teamId,
                format,
                new UpsertTeamPitchCardsRequest(selections));
    }
}
