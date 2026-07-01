package com.project.bluffball.domain.game.controller;

import com.project.bluffball.domain.game.dto.request.BatterCardSelectRequest;
import com.project.bluffball.domain.game.dto.request.MulliganRequest;
import com.project.bluffball.domain.game.dto.request.PitcherCardSelectRequest;
import com.project.bluffball.domain.game.dto.request.SetupNumberRequest;
import com.project.bluffball.domain.game.service.GamePrepService;
import com.project.bluffball.domain.game.service.GameTurnService;
import com.project.bluffball.global.security.AuthenticatedUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * 인게임 WebSocket(STOMP) 컨트롤러.
 *
 * <p>클라이언트는 {@code /ws}로 STOMP 연결 후 {@code /topic/game/{matchSessionId}}를 구독하고,
 * 아래 {@code /app} 경로로 메시지를 전송하여 인게임 각 단계를 진행한다.</p>
 *
 * <p>CONNECT 시 JWT가 필요하며, 구독·전송은 해당 매치 참가자만 허용된다.
 * {@link com.project.bluffball.global.security.WebSocketAuthChannelInterceptor}에서 사전 검증한다.</p>
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

    /** STOMP CONNECT principal에서 userId 추출 */
    private final AuthenticatedUserResolver authenticatedUserResolver;

    /**
     * [Phase 2] 블러핑 고유 숫자 제출.
     *
     * <b>수신 경로:</b> {@code /app/game/{matchSessionId}/setup-numbers}
     */
    @MessageMapping("/game/{matchSessionId}/setup-numbers")
    public void setupNumbers(
            @DestinationVariable String matchSessionId,
            @Payload SetupNumberRequest request,
            Principal principal) {
        Long userId = authenticatedUserResolver.requireUserId(principal);
        gamePrepService.setupNumbers(matchSessionId, userId, request);
    }

    /**
     * [Phase 3] 투수 카드 교체(멀리건) 요청.
     *
     * <b>수신 경로:</b> {@code /app/game/{matchSessionId}/cards/mulligan}
     */
    @MessageMapping("/game/{matchSessionId}/cards/mulligan")
    public void mulligan(
            @DestinationVariable String matchSessionId,
            @Payload MulliganRequest request,
            Principal principal) {
        Long userId = authenticatedUserResolver.requireUserId(principal);
        gamePrepService.processMulligan(matchSessionId, userId, request);
    }

    /**
     * [Phase 4] 투수 구종 및 시작 좌표 선택.
     *
     * <b>수신 경로:</b> {@code /app/game/{matchSessionId}/pitcher/select-card}
     */
    @MessageMapping("/game/{matchSessionId}/pitcher/select-card")
    public void pitcherSelectCard(
            @DestinationVariable String matchSessionId,
            @Payload PitcherCardSelectRequest request,
            Principal principal) {
        Long userId = authenticatedUserResolver.requireUserId(principal);
        gameTurnService.pitcherSelectCard(matchSessionId, userId, request);
    }

    /**
     * [Phase 5] 타자 최종 좌표 및 타이밍 선택 → 타격 이벤트 판정.
     *
     * <b>수신 경로:</b> {@code /app/game/{matchSessionId}/batter/select-card}
     */
    @MessageMapping("/game/{matchSessionId}/batter/select-card")
    public void batterSelectCard(
            @DestinationVariable String matchSessionId,
            @Payload BatterCardSelectRequest request,
            Principal principal) {
        Long userId = authenticatedUserResolver.requireUserId(principal);
        gameTurnService.batterSelectCard(matchSessionId, userId, request);
    }
}
