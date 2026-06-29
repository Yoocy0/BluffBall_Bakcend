package com.project.bluffball.domain.game.service;

import com.project.bluffball.domain.game.config.GameModeRule;
import com.project.bluffball.domain.game.dto.progress.GameProgressApplyResult;
import com.project.bluffball.domain.game.dto.progress.GameTurnOutcome;
import com.project.bluffball.domain.game.dto.response.GameEndEvent;
import com.project.bluffball.domain.game.dto.response.GameStateSnapshot;
import com.project.bluffball.domain.game.dto.response.TurnResultEvent;
import com.project.bluffball.domain.game.enums.TurnResult;
import com.project.bluffball.domain.game.redis.TurnResultSession;
import com.project.bluffball.domain.game.repository.TurnResultSessionRepository;
import com.project.bluffball.domain.game.service.usecase.executor.GameEndExecutor;
import com.project.bluffball.domain.game.service.usecase.executor.GameProgressExecutor;
import com.project.bluffball.domain.game.service.usecase.judgment.GameProgressCalculator;
import com.project.bluffball.domain.game.service.usecase.reader.GameEndReader;
import com.project.bluffball.domain.game.service.usecase.reader.GameProgressReader;
import com.project.bluffball.domain.game.service.usecase.reader.GameStateReader;
import com.project.bluffball.domain.game.service.usecase.reader.TurnResultSessionReader;
import com.project.bluffball.domain.game.service.usecase.validator.GameEndValidator;
import com.project.bluffball.domain.user.record.enums.GameMode;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 야구 경기 진행 서비스 — 턴 판정 결과를 받아 경기 상태를 구성한다.
 *
 * <p>판정 Executor는 {@link com.project.bluffball.domain.game.enums.TurnResult}만 반환하고,
 * 카운트·주자·점수 갱신 및 {@link TurnResultEvent} 클라이언트 전송은 이 Service가 담당한다.</p>
 *
 * <h3>연동</h3>
 * <ul>
 *   <li>{@link GamePrepService} — 준비 완료 시 {@link #initializeGame(String, int)} 호출</li>
 *   <li>{@link GameTurnService} — 타자 선택·판정 직후 {@link #applyTurnResult(String, GameTurnOutcome)} 호출</li>
 * </ul>
 *
 * <p>총 이닝 수와 {@code TurnResult}만 입력받으므로 모드·커스텀 룰이 달라도 동일 인터페이스로 확장 가능하다.</p>
 *
 * @see GamePrepService 준비 단계(숫자 셋업, 카드 드로우·멀리건)
 * @see GameTurnService 반복 턴(투수/타자 선택, 타격 판정)
 */
@Service
@RequiredArgsConstructor
public class GameProgressService {

    private final GameProgressExecutor gameProgressExecutor;
    private final GameEndExecutor gameEndExecutor;
    private final GameProgressReader gameProgressReader;
    private final GameEndReader gameEndReader;
    private final GameStateReader gameStateReader;
    private final TurnResultSessionReader turnResultSessionReader;
    private final TurnResultSessionRepository turnResultSessionRepository;
    private final GameProgressCalculator gameProgressCalculator;
    private final GameEndValidator gameEndValidator;
    private final GameModeRule gameModeRule;
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
            throw new IllegalArgumentException("총 이닝 수는 1 이상이어야 합니다. totalInnings=" + totalInnings);
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
        var situation = gameProgressReader.getSituation(matchSessionId);
        TurnResult effectiveResult = gameProgressCalculator.resolveEffectiveTurnResult(
                outcome.turnResult(), situation);

        // TurnResult별 분기(strike++/ball++/진루/out++ 등) → GameState 갱신
        gameProgressExecutor.applyTurnResult(
                matchSessionId,
                new GameTurnOutcome(effectiveResult, outcome.turnNumber()));

        var snapshot = gameStateReader.getSnapshot(matchSessionId);

        // 판정 부가 정보(좌표·타이밍·주사위)는 Redis TurnResultSession에서 조회
        var turnSession = turnResultSessionReader.getSession(matchSessionId, outcome.turnNumber());
        if (effectiveResult != outcome.turnResult()) {
            turnSession.correctTurnResult(effectiveResult);
            turnResultSessionRepository.save(turnSession);
        }
        publishTurnResultEvent(matchSessionId, effectiveResult, turnSession, snapshot);

        boolean gameOver = handleGameEndIfNeeded(matchSessionId, snapshot);

        return new GameProgressApplyResult(
                snapshot,
                gameOver,
                effectiveResult,
                turnSession.getFinalCoordinateNumber(),
                turnSession.getPitchTiming(),
                turnSession.getDiceResults(),
                outcome.turnNumber());
    }

    /**
     * 경기 종료 시 Reader 조회 → Validator 검증 → Executor DB 저장 → 클라이언트 이벤트 발행.
     */
    private boolean handleGameEndIfNeeded(String matchSessionId, GameStateSnapshot snapshot) {
        if (!gameEndReader.isGameEnded(matchSessionId)) {
            return false;
        }

        if (gameEndReader.isAlreadyArchived(matchSessionId)) {
            publishGameEndEvent(matchSessionId, snapshot);
            return true;
        }

        gameEndValidator.validateArchiveReady(
                gameProgressReader.isInitialized(matchSessionId),
                gameEndReader.isGameEnded(matchSessionId),
                gameEndReader.isAlreadyArchived(matchSessionId));

        var completedSessions = gameEndReader.getCompletedTurnSessions(matchSessionId);
        gameEndValidator.validateCompletedTurnSessions(completedSessions);
        gameEndExecutor.archive(completedSessions);

        publishGameEndEvent(matchSessionId, snapshot);
        return true;
    }

    private void publishTurnResultEvent(String matchSessionId,
                                        TurnResult turnResult,
                                        TurnResultSession turnSession,
                                        GameStateSnapshot snapshot) {
        // turnResult = 판정 enum, snapshot = Calculator 반영 후 스코어보드, turnSession = 좌표·주사위 등
        TurnResultEvent event = new TurnResultEvent(
                turnResult,
                turnSession.getFinalCoordinateNumber(),
                turnSession.getPitchTiming(),
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
                snapshot.thirdBase());
        messagingTemplate.convertAndSend(GAME_TOPIC + matchSessionId + RESULT_TOPIC_SUFFIX, event);
    }

    /**
     * 경기 종료 이벤트를 브로드캐스트한다.
     *
     * <p>TODO: 승자 판정(winnerUserId) — 모드별 홈/어웨이·팀 매핑 후 구현</p>
     */
    private void publishGameEndEvent(String matchSessionId, GameStateSnapshot snapshot) {
        GameEndEvent event = new GameEndEvent(
                snapshot.homeScore(),
                snapshot.awayScore(),
                null);
        messagingTemplate.convertAndSend(GAME_TOPIC + matchSessionId + END_TOPIC_SUFFIX, event);
    }
}
