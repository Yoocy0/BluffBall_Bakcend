package com.project.bluffball.domain.game.service;

import com.project.bluffball.domain.game.config.GameModeRule;
import com.project.bluffball.domain.game.dto.progress.GameProgressApplyResult;
import com.project.bluffball.domain.game.dto.progress.GameTurnOutcome;
import com.project.bluffball.domain.game.dto.response.GameEndEvent;
import com.project.bluffball.domain.game.dto.response.GameStateSnapshot;
import com.project.bluffball.domain.game.service.usecase.executor.GameProgressExecutor;
import com.project.bluffball.domain.game.service.usecase.reader.GameProgressReader;
import com.project.bluffball.domain.game.service.usecase.reader.GameStateReader;
import com.project.bluffball.domain.user.record.enums.GameMode;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 야구 경기 진행 서비스 — 턴 판정 결과를 받아 경기 상태를 구성한다.
 *
 * <p>{@link GamePrepService}와 {@link GameTurnService} 사이의 경기 엔진 레이어이다.
 * 판정(좌표·타이밍·주사위)은 GameTurnService, 야구 룰(카운트·주자·점수·이닝·종료)은 이 Service가 담당한다.</p>
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
    private final GameProgressReader gameProgressReader;
    private final GameStateReader gameStateReader;
    private final GameModeRule gameModeRule;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String GAME_TOPIC = "/topic/game/";
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
     * 한 턴의 판정 결과를 반영한다.
     *
     * <p>{@link GameTurnService}가 타격 판정을 마친 뒤
     * {@link GameTurnOutcome}만 넘기면 야구 룰에 따라 GameState가 갱신된다.</p>
     *
     * @param matchSessionId 매치 세션 ID
     * @param outcome        해당 턴의 최종 판정
     * @return 갱신된 스코어보드 및 경기 종료 여부
     */
    public GameProgressApplyResult applyTurnResult(String matchSessionId, GameTurnOutcome outcome) {
        gameProgressExecutor.applyTurnResult(matchSessionId, outcome);

        var snapshot = gameStateReader.getSnapshot(matchSessionId);
        boolean gameOver = gameProgressReader.isGameOver(matchSessionId);

        if (gameOver) {
            publishGameEndEvent(matchSessionId, snapshot);
        }

        return new GameProgressApplyResult(snapshot, gameOver);
    }

    /**
     * 경기 종료 이벤트를 브로드캐스트한다.
     *
     * <p>TODO: 승자 판정(winnerUserId) — 모드별 홈/어웨이·팀 매핑 후 구현</p>
     */
    private void publishGameEndEvent(String matchSessionId, GameStateSnapshot snapshot) {
        GameEndEvent event = GameEndEvent.builder()
                .homeScore(snapshot.getHomeScore())
                .awayScore(snapshot.getAwayScore())
                .winnerUserId(null)
                .build();
        messagingTemplate.convertAndSend(GAME_TOPIC + matchSessionId + END_TOPIC_SUFFIX, event);
    }
}
