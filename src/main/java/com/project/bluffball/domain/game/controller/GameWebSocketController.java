package com.project.bluffball.domain.game.controller;

import com.project.bluffball.domain.game.dto.request.BatterCardSelectRequest;
import com.project.bluffball.domain.game.dto.request.MulliganRequest;
import com.project.bluffball.domain.game.dto.request.PitcherCardSelectRequest;
import com.project.bluffball.domain.game.dto.request.SetupNumberRequest;
import com.project.bluffball.domain.game.service.GamePrepService;
import com.project.bluffball.domain.game.service.GameTurnService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * 인게임 WebSocket(STOMP) 컨트롤러.
 *
 * <p>클라이언트는 {@code /ws}로 STOMP 연결 후 {@code /topic/game/{matchSessionId}}를 구독하고,
 * 아래 {@code /app} 경로로 메시지를 전송하여 인게임 각 단계를 진행한다.</p>
 *
 * <pre>
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │  인게임 WebSocket 흐름 (싱글 모드 기준 / MVP)                              │
 * ├───┬──────────────────────────────────────────────────────────────────────┤
 * │ 1 │ [투수/타자 → 서버] setup-numbers  /app/game/{id}/setup-numbers       │
 * │   │  └ 블러핑 고유 숫자 제출 (싱글: 4개 필드 동시 / 팀전: 역할별 분리)      │
 * │   │  └ 양측 제출 완료 시 서버가 자동으로 카드 드로우 실행 (Phase 2 진입)    │
 * ├───┼──────────────────────────────────────────────────────────────────────┤
 * │ 2 │ [서버 → 투수] CardHandEvent       /topic/game/{id}                   │
 * │   │  └ 양측 setup-numbers 완료 직후 서버가 초기 카드 패를 드로우하여 전송   │
 * ├───┼──────────────────────────────────────────────────────────────────────┤
 * │ 3 │ [투수 → 서버] cards/mulligan      /app/game/{id}/cards/mulligan      │
 * │   │  └ 교체할 카드 ID 목록 제출 (빈 리스트 = 교체 없이 확정)               │
 * │   │ [서버 → 투수] CardHandEvent       /topic/game/{id}                   │
 * │   │  └ 최종 확정된 카드 패 재전송                                          │
 * ├───┼──────────────────────────────────────────────────────────────────────┤
 * │ 4 │ [투수 → 서버] pitcher/select-card /app/game/{id}/pitcher/select-card │
 * │   │  └ 구종 카드 + 시작 좌표 카드 동시 제출 (UX는 2단계, 전송은 1회)        │
 * │   │ [서버 → 타자] PitcherReadyEvent   /topic/game/{id}                   │
 * │   │  └ 시작 좌표 번호 공개 + 5초 타이머 시작 신호                           │
 * ├───┼──────────────────────────────────────────────────────────────────────┤
 * │ 5 │ [타자 → 서버] batter/select-card  /app/game/{id}/batter/select-card  │
 * │   │  └ 예측 좌표 + 타이밍 카드 제출 (타임아웃 시 isTimeout=true)           │
 * │   │ [서버 → 양측] TurnResultEvent     /topic/game/{id}/result            │
 * │   │  └ 최종 판정 결과 + 업데이트된 카운트/점수 브로드캐스트                  │
 * └───┴──────────────────────────────────────────────────────────────────────┘
 * </pre>
 */
@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private final GamePrepService gamePrepService;
    private final GameTurnService gameTurnService;

    /**
     * [Phase 2] 블러핑 고유 숫자 제출.
     *
     * <p>경기 시작 직후 카드 드로우와 함께 각 플레이어가 1~12 사이에서
     * 자신의 역할에 맞는 비밀 번호를 선택하여 제출한다.</p>
     *
     * <ul>
     *   <li><b>투수</b>: 아웃 번호 5개({@code pitcherOutNumbers}) +
     *       병살 번호 1개({@code pitcherDoublePlayNumber})</li>
     *   <li><b>타자</b>: 3루타 번호 1개({@code batterTripleNumber}) +
     *       홈런 번호 1개({@code batterHomerunNumber})</li>
     *   <li><b>싱글 모드</b>: 한 유저가 4개 필드 모두 제출</li>
     * </ul>
     *
     * <p>양측 제출이 완료되면 카드 교체(Mulligan) 단계로 진행된다.</p>
     *
     * <b>수신 경로:</b> {@code /app/game/{matchSessionId}/setup-numbers}
     */
    @MessageMapping("/game/{matchSessionId}/setup-numbers")
    public void setupNumbers(
            @DestinationVariable String matchSessionId,
            @Payload SetupNumberRequest request) {
        // TODO: userId — Spring Security Principal에서 추출 예정
        Long userId = 1L;
        gamePrepService.setupNumbers(matchSessionId, userId, request);
    }

    /**
     * [Phase 3] 투수 카드 교체(멀리건) 요청.
     *
     * <p>블러핑 숫자 설정 완료 후, 투수가 초기 카드 패에서
     * 원하는 카드를 새 카드로 교체하거나 그대로 확정한다.
     * 멀리건은 경기 당 1회만 허용된다.</p>
     *
     * <ul>
     *   <li>교체 요청: {@code cardIdsToSwap}에 교체 대상 카드 ID 목록 포함</li>
     *   <li>교체 없이 확정: {@code cardIdsToSwap}에 빈 리스트 전송</li>
     * </ul>
     *
     * <p>처리 완료 후 서버는 최종 확정된 카드 패를
     * {@code CardHandEvent}로 투수에게 재전송한다.</p>
     *
     * <b>수신 경로:</b> {@code /app/game/{matchSessionId}/cards/mulligan}<br>
     * <b>송신 경로:</b> {@code /topic/game/{matchSessionId}} → {@code CardHandEvent}
     */
    @MessageMapping("/game/{matchSessionId}/cards/mulligan")
    public void mulligan(
            @DestinationVariable String matchSessionId,
            @Payload MulliganRequest request) {
        // TODO: userId — Spring Security Principal에서 추출 예정
        Long userId = 1L;
        gamePrepService.processMulligan(matchSessionId, userId, request);
    }

    /**
     * [Phase 4] 투수 구종 및 시작 좌표 선택.
     *
     * <p>카드 교체 확정 후, 투수가 구종 카드와 시작 좌표 카드를 선택하여 투구한다.
     * 클라이언트 UX는 2단계(구종 선택 → 좌표 격자 활성화 → 좌표 선택)이나,
     * 서버 전송은 두 값을 한 번에 묶어 1회만 전송한다.</p>
     *
     * <p>처리 완료 후 서버는 타자에게 시작 좌표 번호를 공개하고
     * 5초 타이머 시작을 알리는 {@code PitcherReadyEvent}를 전송한다.</p>
     *
     * <b>수신 경로:</b> {@code /app/game/{matchSessionId}/pitcher/select-card}<br>
     * <b>송신 경로:</b> {@code /topic/game/{matchSessionId}} → {@code PitcherReadyEvent} (타자 전용)
     */
    @MessageMapping("/game/{matchSessionId}/pitcher/select-card")
    public void pitcherSelectCard(
            @DestinationVariable String matchSessionId,
            @Payload PitcherCardSelectRequest request) {
        // TODO: userId — Spring Security Principal에서 추출 예정
        Long userId = 1L;
        gameTurnService.pitcherSelectCard(matchSessionId, userId, request);
    }

    /**
     * [Phase 5] 타자 최종 좌표 및 타이밍 선택 → 타격 이벤트 판정.
     *
     * <p>{@code PitcherReadyEvent} 수신 후 5초 타이머 내에 타자가
     * 투수의 최종 좌표와 타이밍을 예측하여 선택한다.
     * 타임아웃 시 클라이언트가 {@code isTimeout=true}로 자동 전송한다.</p>
     *
     * <p>서버는 수신 즉시 아래 순서로 타격 이벤트를 계산한다:</p>
     * <ol>
     *   <li>투수 최종 좌표 = 시작 좌표 + 구종 카드 변화값(changeAmount × direction)</li>
     *   <li>좌표 일치 여부 확인 (타자 선택 좌표 vs 투수 최종 좌표)</li>
     *   <li>타이밍 일치 여부 → 주사위 개수 결정 (완벽 일치: 2개, 1칸 어긋남: 1개, 불일치: 헛스윙)</li>
     *   <li>주사위 롤 → 블러핑 숫자 대조 → {@link com.project.bluffball.domain.game.enums.TurnResult} 확정</li>
     *   <li>폭투 조건 확인 (투수 최종 좌표 0 + 타자 좌표 0 선택 시)</li>
     * </ol>
     *
     * <p>판정 완료 후 투수와 타자 양측에 {@code TurnResultEvent}를 브로드캐스트한다.</p>
     *
     * <b>수신 경로:</b> {@code /app/game/{matchSessionId}/batter/select-card}<br>
     * <b>송신 경로:</b> {@code /topic/game/{matchSessionId}/result} → {@code TurnResultEvent} (양측)
     */
    @MessageMapping("/game/{matchSessionId}/batter/select-card")
    public void batterSelectCard(
            @DestinationVariable String matchSessionId,
            @Payload BatterCardSelectRequest request) {
        // TODO: userId — Spring Security Principal에서 추출 예정
        Long userId = 1L;
        gameTurnService.batterSelectCard(matchSessionId, userId, request);
    }
}
