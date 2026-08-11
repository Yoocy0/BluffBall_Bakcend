package com.project.bluffball.domain.game.service;

import com.project.bluffball.domain.game.config.GameModeRule;
import com.project.bluffball.domain.game.dto.progress.GameProgressApplyResult;
import com.project.bluffball.domain.game.dto.progress.GameTurnOutcome;
import com.project.bluffball.domain.game.dto.response.GameEndEvent;
import com.project.bluffball.domain.game.dto.response.GameStateSnapshot;
import com.project.bluffball.domain.game.dto.response.TurnResultEvent;
import com.project.bluffball.domain.game.enums.TurnResult;
import com.project.bluffball.domain.game.event.HalfInningChangedEvent;
import com.project.bluffball.domain.game.redis.TurnResultSession;
import com.project.bluffball.domain.game.repository.TurnResultSessionRepository;
import com.project.bluffball.domain.game.service.MatchService;
import com.project.bluffball.domain.game.service.usecase.executor.BatterAdvanceExecutor;
import com.project.bluffball.domain.game.service.usecase.executor.GameEndExecutor;
import com.project.bluffball.domain.game.service.usecase.executor.GameProgressExecutor;
import com.project.bluffball.domain.game.service.usecase.executor.LeagueMatchResultMarkExecutor;
import com.project.bluffball.domain.game.service.usecase.judgment.GameProgressCalculator;
import com.project.bluffball.domain.game.service.usecase.reader.GameEndReader;
import com.project.bluffball.domain.game.service.usecase.reader.GameProgressReader;
import com.project.bluffball.domain.game.service.usecase.reader.GameStateReader;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.game.service.usecase.reader.TurnResultSessionReader;
import com.project.bluffball.domain.game.service.usecase.validator.GameEndValidator;
import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.service.LeagueMatchResultService;
import com.project.bluffball.domain.user.record.enums.GameMode;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 야구 경기 진행 서비스 — 턴 판정 결과를 받아 경기 상태를 구성한다.
 *
 * <p>판정 Executor는 {@link com.project.bluffball.domain.game.enums.TurnResult}만 반환하고,
 * 카운트·주자·점수 갱신 및 {@link TurnResultEvent} 클라이언트 전송은 이 Service가 담당한다.
 * 동점 연장·끝내기 등 종료 판정 규칙 자체는 {@link GameProgressCalculator}에 있다.</p>
 *
 * <h3>연동</h3>
 * <ul>
 *   <li>{@link GamePrepService} — 준비 완료 시 {@link #initializeGame(String, int)} 호출</li>
 *   <li>{@link GameTurnService} — 타자 선택·판정 직후 {@link #applyTurnResult(String, GameTurnOutcome)} 호출</li>
 * </ul>
 *
 * <h3>종료 흐름</h3>
 * <ul>
 *   <li>끝내기 — Calculator가 {@code gameOver}를 켜면 하프이닝 전환 없이 {@link #handleGameEndIfNeeded}로 종료</li>
 *   <li>동점 연장 — 말 3아웃 후 동점이면 다음 이닝으로 진행되며 {@link HalfInningChangedEvent}로 공수 교대</li>
 * </ul>
 *
 * <p>총 이닝 수와 {@code TurnResult}만 입력받으므로 모드·커스텀 룰이 달라도 동일 인터페이스로 확장 가능하다.</p>
 *
 * @see GamePrepService 준비 단계(숫자 셋업, 카드 드로우·멀리건)
 * @see GameTurnService 반복 턴(투수/타자 선택, 타격 판정)
 * @see GameProgressCalculator 동점 연장·끝내기 규칙
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameProgressService {

    private final ApplicationEventPublisher eventPublisher;

    private final GameProgressExecutor gameProgressExecutor;
    private final BatterAdvanceExecutor batterAdvanceExecutor;
    private final GameEndExecutor gameEndExecutor;
    private final GameProgressReader gameProgressReader;
    private final GameEndReader gameEndReader;
    private final GameStateReader gameStateReader;
    private final TurnResultSessionReader turnResultSessionReader;
    private final TurnResultSessionRepository turnResultSessionRepository;
    private final GameProgressCalculator gameProgressCalculator;
    private final GameEndValidator gameEndValidator;
    private final GameModeRule gameModeRule;
    private final MatchInfoReader matchInfoReader;
    private final MatchService matchService;
    private final LeagueMatchResultService leagueMatchResultService;
    private final LeagueMatchResultMarkExecutor leagueMatchResultMarkExecutor;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String GAME_TOPIC = "/topic/game/";
    private static final String RESULT_TOPIC_SUFFIX = "/result";
    private static final String END_TOPIC_SUFFIX = "/end";

    /**
     * 경기 진행을 초기화한다.
     *
     * <p>매치 생성 또는 {@link GamePrepService} 준비 완료 시점에
     * 해당 게임의 총 이닝 수를 전달받아 호출한다.</p>
     *
     * @param matchSessionId 매치 세션 ID
     * @param totalInnings   총 이닝 수 (커스텀 모드는 매치 설정값)
     */
    public void initializeGame(String matchSessionId, int totalInnings) {
        if (totalInnings <= 0) {
            throw new BadRequestException(ErrorCode.GAME_INVALID_INNINGS, "totalInnings=" + totalInnings);
        }
        gameProgressExecutor.initialize(matchSessionId, totalInnings);
    }

    /**
     * 게임 모드 기본 이닝 수로 경기 진행을 초기화한다.
     *
     * @param matchSessionId 매치 세션 ID
     * @param gameMode       게임 모드
     */
    public void initializeGame(String matchSessionId, GameMode gameMode) {
        initializeGame(matchSessionId, gameModeRule.getDefaultInnings(gameMode));
    }

    /**
     * 경기가 아직 시작되지 않았을 때만 초기화한다.
     *
     * <p>{@link GamePrepService} 멀리건 확정 시 호출한다.
     * 투수 교체 후 멀리건이 반복되어도 진행 중인 경기 상태는 리셋하지 않는다.</p>
     */
    public void ensureGameStarted(String matchSessionId, GameMode gameMode) {
        if (!gameProgressReader.isInitialized(matchSessionId)) {
            initializeGame(matchSessionId, gameMode);
        }
    }

    /**
     * 한 턴의 판정 결과를 반영하고 클라이언트에 {@link TurnResultEvent}를 전송한다.
     *
     * @param matchSessionId 매치 세션 ID
     * @param outcome        해당 턴의 최종 {@link com.project.bluffball.domain.game.enums.TurnResult} 및 턴 번호
     */
    public GameProgressApplyResult applyTurnResult(String matchSessionId, GameTurnOutcome outcome) {
        var situationBefore = gameProgressReader.getSituation(matchSessionId);
        TurnResult effectiveResult = gameProgressCalculator.resolveEffectiveTurnResult(
                outcome.turnResult(), situationBefore);

        // TurnResult별 분기(strike++/ball++/진루/out++ 등) → GameState 갱신
        gameProgressExecutor.applyTurnResult(
                matchSessionId,
                new GameTurnOutcome(effectiveResult, outcome.turnNumber()));

        // 타석 종료 시 타순 전진 (공수 교대 전 — 다음 공격 이닝 이어가기에 사용)
        batterAdvanceExecutor.advanceIfPlateAppearanceEnded(matchSessionId, effectiveResult);

        var snapshot = gameStateReader.getSnapshot(matchSessionId);

        // 판정 부가 정보(좌표·타이밍·주사위)는 Redis TurnResultSession에서 조회
        var turnSession = turnResultSessionReader.getSession(matchSessionId, outcome.turnNumber());
        if (effectiveResult != outcome.turnResult()) {
            turnSession.correctTurnResult(effectiveResult);
            turnResultSessionRepository.save(turnSession);
        }

        boolean halfInningChanged = situationBefore.currentInning() != snapshot.inning()
                || situationBefore.isTop() != snapshot.isTop();
        boolean gameOver = gameProgressReader.isGameOver(matchSessionId);

        // CardHand(역할·멀리건)을 TurnResult보다 먼저 보내 클라이언트가 역할을 동기화할 수 있게 한다.
        if (halfInningChanged && !gameOver) {
            eventPublisher.publishEvent(new HalfInningChangedEvent(matchSessionId));
        }

        Long pitcherUserId = matchInfoReader.getPitcherUserId(matchSessionId);
        publishTurnResultEvent(
                matchSessionId,
                effectiveResult,
                turnSession,
                snapshot,
                pitcherUserId,
                halfInningChanged,
                gameOver);

        boolean archivedGameOver = handleGameEndIfNeeded(matchSessionId, snapshot);

        return new GameProgressApplyResult(
                snapshot,
                archivedGameOver || gameOver,
                effectiveResult,
                turnSession.getFinalCoordinateNumber(),
                turnSession.getPitchTiming(),
                turnSession.getDiceResults(),
                outcome.turnNumber());
    }

    /**
     * 경기 종료 시 DB 아카이브·큐 정리·클라이언트 이벤트 발행.
     *
     * <p>정상 종료·몰수패 공통 처리. 턴 기록이 없으면 아카이브는 생략한다.</p>
     */
    public void finalizeGameEnd(String matchSessionId, Long winnerUserId) {
        if (!gameEndReader.isGameEnded(matchSessionId)) {
            return;
        }

        GameStateSnapshot snapshot = gameStateReader.getSnapshot(matchSessionId);
        publishGameEndEvent(matchSessionId, snapshot, winnerUserId);

        // GameMode.BOT(연습 vs-bot) — 보상·래더 사이드이펙트 스킵 필터
        // isPracticeBotMatch는 practiceBotMatch 플래그 또는 gameMode==BOT 으로 판정한다.
        if (matchInfoReader.isPracticeBotMatch(matchSessionId)
                || matchInfoReader.getGameMode(matchSessionId) == GameMode.BOT) {
            // TODO: Showdown에 보상/전적/래더가 추가되면 여기서 스킵한다.
            // 현재 리그 순위 반영은 isLeagueMatch로 이미 제외되며, InningLog 아카이브는 디버깅용으로 유지한다.
            log.info("practice/bot match end (GameMode.BOT) — skip reward/standings side effects matchSessionId={}",
                    matchSessionId);
        }

        // 리그 순위 반영 (멱등)
        applyLeagueStandingsIfNeeded(matchSessionId, snapshot);

        if (gameEndReader.isAlreadyArchived(matchSessionId)) {
            return;
        }

        var completedSessions = gameEndReader.getCompletedTurnSessions(matchSessionId);
        if (completedSessions.isEmpty()) {
            matchService.clearQueueEntriesForMatch(matchSessionId);
            return;
        }

        gameEndValidator.validateArchiveReady(
                gameProgressReader.isInitialized(matchSessionId),
                true,
                false);
        gameEndValidator.validateCompletedTurnSessions(completedSessions);
        gameEndExecutor.archive(completedSessions);
        matchService.clearQueueEntriesForMatch(matchSessionId);
    }

    /**
     * 리그 매치면 티어 점수·매치 보상을 반영한다.
     *
     * @param matchSessionId 매치 세션 ID
     * @param snapshot 최종 스코어
     */
    private void applyLeagueStandingsIfNeeded(String matchSessionId, GameStateSnapshot snapshot) {
        if (!matchInfoReader.isLeagueMatch(matchSessionId)) {
            return;
        }
        if (matchInfoReader.isLeagueResultApplied(matchSessionId)) {
            return;
        }
        Long homeTeamId = matchInfoReader.getHomeTeamId(matchSessionId);
        Long awayTeamId = matchInfoReader.getAwayTeamId(matchSessionId);
        LeagueFormat format = toLeagueFormat(matchInfoReader.getGameMode(matchSessionId));
        if (format == null || homeTeamId == null || awayTeamId == null) {
            log.warn("리그 결과 반영 스킵 — format/team 누락 matchSessionId={}", matchSessionId);
            return;
        }
        leagueMatchResultService.applyMatchResult(
                format,
                homeTeamId,
                awayTeamId,
                snapshot.homeScore(),
                snapshot.awayScore());
        leagueMatchResultMarkExecutor.markApplied(matchSessionId);
    }

    /**
     * GameMode → LeagueFormat 매핑.
     *
     * @param gameMode 게임 모드
     * @return 리그 포맷, 리그가 아니면 null
     */
    private LeagueFormat toLeagueFormat(GameMode gameMode) {
        if (gameMode == null) {
            return null;
        }
        return switch (gameMode) {
            case COMPACT_LEAGUE -> LeagueFormat.COMPACT;
            case FULL_LEAGUE -> LeagueFormat.FULL;
            default -> null;
        };
    }

    /**
     * 경기 종료 시 Reader 조회 → Validator 검증 → Executor DB 저장 → 클라이언트 이벤트 발행.
     */
    private boolean handleGameEndIfNeeded(String matchSessionId, GameStateSnapshot snapshot) {
        if (!gameEndReader.isGameEnded(matchSessionId)) {
            return false;
        }

        finalizeGameEnd(matchSessionId, resolveWinnerUserId(matchSessionId, snapshot));
        return true;
    }

    private Long resolveWinnerUserId(String matchSessionId, GameStateSnapshot snapshot) {
        if (snapshot.homeScore() > snapshot.awayScore()) {
            return matchInfoReader.getHomeUserId(matchSessionId);
        }
        if (snapshot.awayScore() > snapshot.homeScore()) {
            return matchInfoReader.getAwayUserId(matchSessionId);
        }
        return null;
    }

    private void publishTurnResultEvent(String matchSessionId,
                                        TurnResult turnResult,
                                        TurnResultSession turnSession,
                                        GameStateSnapshot snapshot,
                                        Long pitcherUserId,
                                        boolean halfInningChanged,
                                        boolean gameOver) {
        TurnResultEvent event = new TurnResultEvent(
                turnResult,
                turnSession.getFinalCoordinateNumber(),
                turnSession.getPitchTiming(),
                null, // 구종명 비공개 — 최종 좌표·타이밍·판정만 공개
                turnSession.getDiceResults(),
                snapshot.inning(),
                snapshot.isTop(),
                snapshot.homeScore(),
                snapshot.awayScore(),
                snapshot.balls(),
                snapshot.strikes(),
                snapshot.outs(),
                snapshot.firstBase(),
                snapshot.secondBase(),
                snapshot.thirdBase(),
                pitcherUserId,
                halfInningChanged,
                gameOver);
        messagingTemplate.convertAndSend(GAME_TOPIC + matchSessionId + RESULT_TOPIC_SUFFIX, event);
    }

    /**
     * 경기 종료 이벤트를 브로드캐스트한다.
     */
    private void publishGameEndEvent(String matchSessionId, GameStateSnapshot snapshot, Long winnerUserId) {
        GameEndEvent event = new GameEndEvent(
                snapshot.homeScore(),
                snapshot.awayScore(),
                winnerUserId);
        messagingTemplate.convertAndSend(GAME_TOPIC + matchSessionId + END_TOPIC_SUFFIX, event);
    }
}
