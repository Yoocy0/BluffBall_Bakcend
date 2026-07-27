package com.project.bluffball.domain.game.service;

import com.project.bluffball.domain.game.dto.request.LeagueMatchQueueJoinRequest;
import com.project.bluffball.domain.game.dto.response.MatchFoundEvent;
import com.project.bluffball.domain.game.dto.response.MatchJoinResponse;
import com.project.bluffball.domain.game.enums.MatchJoinDecision;
import com.project.bluffball.domain.game.enums.MatchQueueState;
import com.project.bluffball.domain.game.service.usecase.executor.LeagueMatchCreateExecutor;
import com.project.bluffball.domain.game.service.usecase.executor.MatchQueueExecutor;
import com.project.bluffball.domain.game.service.usecase.reader.MatchQueueReader;
import com.project.bluffball.domain.game.service.usecase.validator.LeagueMatchValidator;
import com.project.bluffball.domain.game.service.usecase.validator.MatchQueueValidator;
import com.project.bluffball.domain.game.config.GameModeRule;
import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.domain.league.service.usecase.reader.TeamLeagueProgressReader;
import com.project.bluffball.domain.league.service.usecase.validator.LeagueProgressValidator;
import com.project.bluffball.domain.team.dto.response.TeamPitchCardsResponse;
import com.project.bluffball.domain.team.service.usecase.reader.TeamLineupReader;
import com.project.bluffball.domain.team.service.usecase.reader.TeamMemberReader;
import com.project.bluffball.domain.team.service.usecase.reader.TeamPitchCardsReader;
import com.project.bluffball.domain.team.service.usecase.validator.TeamMembershipValidator;
import com.project.bluffball.domain.user.record.enums.GameMode;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.MatchNotFoundException;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 리그 매칭 큐 서비스.
 *
 * <p>format × tier 단위 FIFO 매칭. 선진입 팀이 홈이다.
 * Entity·Repository에 직접 접근하지 않는다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeagueMatchService {

    private static final String USER_MATCH_TOPIC_PREFIX = "/topic/user/";

    private final MatchQueueValidator matchQueueValidator;
    private final LeagueMatchValidator leagueMatchValidator;
    private final LeagueProgressValidator leagueProgressValidator;
    private final TeamMembershipValidator teamMembershipValidator;

    private final MatchQueueReader matchQueueReader;
    private final TeamMemberReader teamMemberReader;
    private final TeamLineupReader teamLineupReader;
    private final TeamPitchCardsReader teamPitchCardsReader;
    private final TeamLeagueProgressReader teamLeagueProgressReader;
    private final GameModeRule gameModeRule;

    private final MatchQueueExecutor matchQueueExecutor;
    private final LeagueMatchCreateExecutor leagueMatchCreateExecutor;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 리그 매칭 큐에 진입한다.
     *
     * @param userId 요청 유저 ID (리더)
     * @param request format·tier
     * @return WAITING 또는 MATCHED 응답
     */
    public MatchJoinResponse joinQueue(Long userId, LeagueMatchQueueJoinRequest request) {
        LeagueFormat format = request.format();
        LeagueTier tier = request.tier();
        GameMode gameMode = toGameMode(format);

        Long teamId = requireTeamId(userId);
        teamMembershipValidator.validateLeader(teamMemberReader.isLeader(teamId, userId));

        // 현재 소속 티어와 동일한 큐만 허용
        leagueProgressValidator.validateMatchingTier(
                teamLeagueProgressReader.isCurrentTier(teamId, format, tier));

        int rosterSize = gameModeRule.getRosterSize(gameMode);
        leagueMatchValidator.validateReady(
                teamLineupReader.exists(teamId, format),
                teamLineupReader.hasStartingPitcher(teamId, format),
                teamPitchCardsReader.isComplete(teamId, format, rosterSize));

        matchQueueExecutor.evictStaleMatchedEntry(userId);
        matchQueueValidator.validateJoinAllowed(matchQueueReader.hasQueueEntry(userId));

        Long opponentUserId = matchQueueExecutor.pollOpponentOrEnqueue(userId, gameMode, tier);
        MatchJoinDecision decision = matchQueueValidator.resolveJoinDecision(opponentUserId, userId);

        return switch (decision) {
            case WAIT -> enqueueAndWait(userId, gameMode, tier, teamId);
            case MATCH -> completeMatch(opponentUserId, userId, gameMode, tier, teamId, format);
        };
    }

    /**
     * 리그 매칭 큐를 취소한다.
     *
     * @param userId 요청 유저 ID
     */
    public void cancelQueue(Long userId) {
        if (!matchQueueReader.hasQueueEntry(userId)) {
            throw new MatchNotFoundException(ErrorCode.MATCH_NOT_FOUND);
        }

        MatchQueueState state = matchQueueReader.getQueueState(userId);
        if (state == MatchQueueState.WAITING) {
            GameMode gameMode = matchQueueReader.getGameMode(userId);
            LeagueTier tier = matchQueueReader.getLeagueTier(userId);
            matchQueueExecutor.cancelWaiting(userId, gameMode, tier);
        } else {
            matchQueueExecutor.clearEntryIfPresent(userId);
        }
        log.info("리그 매칭 큐 취소 userId={} state={}", userId, state);
    }

    /**
     * 대기 등록 후 WAITING 응답을 반환한다.
     *
     * @param userId 유저 ID
     * @param gameMode 게임 모드
     * @param tier 티어
     * @param teamId 팀 ID
     * @return WAITING 응답
     */
    private MatchJoinResponse enqueueAndWait(
            Long userId,
            GameMode gameMode,
            LeagueTier tier,
            Long teamId) {
        matchQueueExecutor.registerWaitingLeague(userId, gameMode, tier, teamId);
        log.info("리그 매칭 큐 대기 userId={} mode={} tier={} teamId={}",
                userId, gameMode, tier, teamId);
        return MatchJoinResponse.waiting();
    }

    /**
     * 매칭을 성사시키고 홈(선진입)에게 알린다.
     *
     * @param homeLeaderUserId 선진입 리더
     * @param awayLeaderUserId 후진입 리더
     * @param gameMode 게임 모드
     * @param tier 티어
     * @param awayTeamId 후진입 팀 ID
     * @param format 리그 포맷
     * @return MATCHED 응답
     */
    private MatchJoinResponse completeMatch(
            Long homeLeaderUserId,
            Long awayLeaderUserId,
            GameMode gameMode,
            LeagueTier tier,
            Long awayTeamId,
            LeagueFormat format) {

        matchQueueValidator.validateOpponentEntryExists(matchQueueReader.hasQueueEntry(homeLeaderUserId));

        Long homeTeamId = matchQueueReader.getTeamId(homeLeaderUserId);
        leagueMatchValidator.validateDifferentTeams(homeTeamId, awayTeamId);
        leagueProgressValidator.validateMatchingTier(
                teamLeagueProgressReader.isCurrentTier(homeTeamId, format, tier));
        leagueProgressValidator.validateMatchingTier(
                teamLeagueProgressReader.isCurrentTier(awayTeamId, format, tier));

        List<Long> homeBattingOrder = teamLineupReader.getUserIds(homeTeamId, format);
        List<Long> awayBattingOrder = teamLineupReader.getUserIds(awayTeamId, format);
        Long homeStartingPitcher = teamLineupReader.getStartingPitcherUserId(homeTeamId, format);
        Long awayStartingPitcher = teamLineupReader.getStartingPitcherUserId(awayTeamId, format);
        TeamPitchCardsResponse homePitchCards = teamPitchCardsReader.getPitchCardsResponse(homeTeamId, format);
        TeamPitchCardsResponse awayPitchCards = teamPitchCardsReader.getPitchCardsResponse(awayTeamId, format);

        String matchSessionId = leagueMatchCreateExecutor.execute(
                gameMode,
                tier,
                homeTeamId,
                awayTeamId,
                homeLeaderUserId,
                awayLeaderUserId,
                homeStartingPitcher,
                awayStartingPitcher,
                homeBattingOrder,
                awayBattingOrder,
                homePitchCards,
                awayPitchCards);

        matchQueueExecutor.markMatched(homeLeaderUserId, matchSessionId);

        notifyMatchFound(homeLeaderUserId, matchSessionId);

        log.info("리그 매칭 성사 matchSessionId={} homeTeamId={} awayTeamId={} mode={} tier={}",
                matchSessionId, homeTeamId, awayTeamId, gameMode, tier);
        return MatchJoinResponse.matched(matchSessionId);
    }

    /**
     * 매칭 성사 알림을 전송한다.
     *
     * @param userId 수신 유저 ID
     * @param matchSessionId 매치 세션 ID
     */
    private void notifyMatchFound(Long userId, String matchSessionId) {
        messagingTemplate.convertAndSend(
                USER_MATCH_TOPIC_PREFIX + userId + "/match",
                new MatchFoundEvent(matchSessionId));
    }

    /**
     * 유저의 소속 팀 ID를 조회한다.
     *
     * @param userId 유저 ID
     * @return 팀 ID
     */
    private Long requireTeamId(Long userId) {
        return teamMemberReader.findTeamIdByUserId(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TEAM_NOT_FOUND, "userId=" + userId));
    }

    /**
     * LeagueFormat → GameMode 매핑.
     *
     * @param format 리그 구분
     * @return 게임 모드
     */
    private GameMode toGameMode(LeagueFormat format) {
        return switch (format) {
            case COMPACT -> GameMode.COMPACT_LEAGUE;
            case FULL -> GameMode.FULL_LEAGUE;
        };
    }
}
