package com.project.bluffball.domain.game.service;

import com.project.bluffball.domain.game.dto.progress.GameProgressApplyResult;
import com.project.bluffball.domain.game.dto.progress.GameTurnOutcome;
import com.project.bluffball.domain.game.dto.request.BatterCardSelectRequest;
import com.project.bluffball.domain.game.dto.request.PitcherCardSelectRequest;
import com.project.bluffball.domain.game.dto.response.PitcherReadyEvent;
import com.project.bluffball.domain.game.enums.TurnResult;
import com.project.bluffball.domain.game.service.usecase.executor.BatterCardSelectExecutor;
import com.project.bluffball.domain.game.service.usecase.executor.PitcherCardSelectExecutor;
import com.project.bluffball.domain.game.service.usecase.reader.GameProgressReader;
import com.project.bluffball.domain.game.service.usecase.reader.GameStateReader;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.game.service.usecase.reader.TurnResultSessionReader;
import com.project.bluffball.domain.game.service.usecase.validator.BatterCardSelectValidator;
import com.project.bluffball.domain.game.service.usecase.validator.GameProgressValidator;
import com.project.bluffball.domain.game.service.usecase.validator.PitcherCardSelectValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 인게임 반복 턴 실행 서비스.
 *
 * <p>투수/타자 카드 선택, 타격 판정, 결과 브로드캐스트를 담당한다.
 * 한 게임에서 수십~수백 번 반복 호출된다.</p>
 *
 * <p>Entity·Repository에 직접 접근하지 않는다.
 * 모든 검증은 Validator, 상태 변경은 Executor, 조회는 Reader에 위임한다.</p>
 *
 * @see GamePrepService 준비 단계(숫자 셋업, 카드 드로우·멀리건)
 * @see GameProgressService 경기 진행(턴 결과 → 야구 룰 반영 · TurnResultEvent 전송)
 */
@Service
@RequiredArgsConstructor
public class GameTurnService {

    private final PitcherCardSelectValidator pitcherCardSelectValidator;
    private final PitcherCardSelectExecutor pitcherCardSelectExecutor;
    private final BatterCardSelectValidator batterCardSelectValidator;
    private final BatterCardSelectExecutor batterCardSelectExecutor;
    private final GameProgressService gameProgressService;
    private final GameProgressValidator gameProgressValidator;
    private final GameProgressReader gameProgressReader;
    private final MatchInfoReader matchInfoReader;
    private final TurnResultSessionReader turnResultSessionReader;
    private final GameStateReader gameStateReader;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String GAME_TOPIC = "/topic/game/";

    /**
     * 투수 구종 카드 및 시작 좌표 카드 선택을 처리한다.
     *
     * <p>처리 완료 후 타자에게 시작 좌표 번호만 공개하는 {@code PitcherReadyEvent}를 전송한다.
     * 최종 좌표와 구종 타이밍은 타자 선택 후 {@code TurnResultEvent}에서 공개하며, 구종명은 공개하지 않는다.</p>
     */
    public void pitcherSelectCard(String matchSessionId, Long userId, PitcherCardSelectRequest request) {
        // 현재 게임의 상태가 유효한지 검증
        gameProgressValidator.validateGameActive(
                gameProgressReader.isInitialized(matchSessionId),
                gameProgressReader.isGameOver(matchSessionId));
        gameProgressValidator.validateSetupComplete(
                matchInfoReader.isSetupNumbersComplete(matchSessionId));

        List<Long> hand = matchInfoReader.getPitcherCardHand(matchSessionId);
        Long pitcherUserId = matchInfoReader.getPitcherUserId(matchSessionId);
        pitcherCardSelectValidator.validate(
                hand,
                pitcherUserId,
                userId,
                request.pitchCardId(),
                request.coordinateCardId(),
                matchInfoReader.isMulliganDoneForUser(matchSessionId, pitcherUserId),
                turnResultSessionReader.isPitcherSelectionComplete(matchSessionId));

        // 투수가 선택한 시작 좌표
        int startCoordinateNumber = pitcherCardSelectExecutor.execute(
                matchSessionId,
                request.pitchCardId(),
                request.coordinateCardId());

        // 5. 타자에게 투수의 투구 준비 완료를 알리는 이벤트 발행 (심리전을 위해 '시작 좌표'만 선공개)
        PitcherReadyEvent event = new PitcherReadyEvent(startCoordinateNumber);
        messagingTemplate.convertAndSend(GAME_TOPIC + matchSessionId, event);
    }

    /**
     * 타자 예측 좌표 및 타이밍 선택을 처리하고 타격 이벤트를 판정한다.
     *
     * <p>판정 완료 후 {@link GameProgressService}가 {@code TurnResultEvent}를 브로드캐스트한다.
     * {@code responseTimeSec}이 5초를 초과하면 스윙 미발동(스트라이크)으로 처리한다.</p>
     */
    public GameProgressApplyResult batterSelectCard(String matchSessionId, Long userId, BatterCardSelectRequest request) {
        // 해당 게임의 유효성 검증
        gameProgressValidator.validateGameActive(
                gameProgressReader.isInitialized(matchSessionId),
                gameProgressReader.isGameOver(matchSessionId));

        /*  타자의 선택 유효성 검증
        *   응답 시간 초과 여부, 유효 좌표/타이밍 선택 여부 검증
        *   투수/타자의 선택 완료 여부 검증
        * */
        Long batterUserId = matchInfoReader.getCurrentBatterUserId(matchSessionId);
        batterCardSelectValidator.validate(
                batterUserId,
                userId,
                request.responseTimeSec(),
                request.batterCoordinateNumber(),
                request.timing(),
                turnResultSessionReader.isPitcherSelectionComplete(matchSessionId),
                turnResultSessionReader.isBatterSelectionComplete(matchSessionId));

        // advanceTurn() 전 턴 번호 — GameProgressService가 해당 턴 세션·결과와 매핑
        int completedTurnNumber = gameStateReader.getTurnNumber(matchSessionId);

        // [판정] 1차(좌표·타이밍) + 2차(주사위·블러핑) → TurnResult만 반환
        TurnResult turnResult = batterCardSelectExecutor.execute(
                matchSessionId,
                request.batterCoordinateNumber(),
                request.timing(),
                request.responseTimeSec());

        // [경기 반영 + 클라이언트 전송] TurnResult → GameState 갱신 · TurnResultEvent 브로드캐스트
        return gameProgressService.applyTurnResult(
                matchSessionId,
                new GameTurnOutcome(turnResult, completedTurnNumber));
    }
}