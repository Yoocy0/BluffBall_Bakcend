package com.project.bluffball.domain.game.service;

import com.project.bluffball.domain.game.dto.request.MulliganRequest;
import com.project.bluffball.domain.game.dto.request.SetupNumberRequest;
import com.project.bluffball.domain.game.dto.response.CardHandEvent;
import com.project.bluffball.domain.game.dto.response.CardInfo;
import com.project.bluffball.domain.game.event.SetupNumbersSubmittedEvent;
import com.project.bluffball.domain.game.service.usecase.executor.CardHandDrawExecutor;
import com.project.bluffball.domain.game.service.usecase.executor.MulliganExecutor;
import com.project.bluffball.domain.game.service.usecase.executor.SetupNumberExecutor;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.game.service.usecase.reader.PitchCardReader;
import com.project.bluffball.domain.game.service.usecase.validator.MulliganValidator;
import com.project.bluffball.domain.game.service.usecase.validator.SetupNumberValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 인게임 준비 단계 서비스.
 *
 * <p>블러핑 숫자 셋업, 카드 드로우(초기 뽑기 + 멀리건)를 담당한다.
 * 투수 교체 시마다 drawCardHand → processMulligan 흐름이 반복될 수 있다.</p>
 *
 * <p>Entity·Repository에 직접 접근하지 않는다.
 * 모든 검증은 Validator, 상태 변경은 Executor, 조회는 Reader에 위임한다.</p>
 *
 * <p>WebSocket 이벤트 발행(SimpMessagingTemplate)은 이 Service에서만 수행한다.</p>
 *
 * @see GameTurnService 반복 턴 실행(투수/타자 카드 선택, 타격 판정)
 * @see GameProgressService 경기 진행(턴 결과 → 야구 룰 반영)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GamePrepService {

    private final SetupNumberValidator setupNumberValidator;
    private final SetupNumberExecutor setupNumberExecutor;
    private final MulliganValidator mulliganValidator;
    private final MulliganExecutor mulliganExecutor;
    private final CardHandDrawExecutor cardHandDrawExecutor;
    private final MatchInfoReader matchInfoReader;
    private final PitchCardReader pitchCardReader;
    private final GameProgressService gameProgressService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;

    private static final String GAME_TOPIC = "/topic/game/";

    /**
     * 블러핑 숫자를 제출한다.
     *
     * <p>저장 완료 후 {@link SetupNumbersSubmittedEvent}를 발행한다.
     * 카드 드로우 시작 여부는 이 메서드가 판단하지 않으며,
     * {@link com.project.bluffball.domain.game.service.listener.GamePhaseListener}가 처리한다.</p>
     */
    public void setupNumbers(String matchSessionId, Long userId, SetupNumberRequest request) {
        log.info("setup-numbers 저장 matchSessionId={} userId={}", matchSessionId, userId);

        // 주사위 눈금의 합 예측 값 유효성 검증
        setupNumberValidator.validate(request);
        // 매치 조회(Reader) 후 블러핑 숫자 저장 — Executor는 쓰기만 담당
        setupNumberExecutor.save(
                matchInfoReader.getById(matchSessionId),
                userId,
                request);
        // 예측값 셋업 완료 시 초기 드로우 이벤트 진행
        eventPublisher.publishEvent(new SetupNumbersSubmittedEvent(matchSessionId));
    }

    /**
     * 초기 카드 패를 뽑아 MatchInfo에 저장하고 투수에게 CardHandEvent를 전송한다.
     *
     * <p>양측 setup-numbers 완료 시 {@link com.project.bluffball.domain.game.service.listener.GamePhaseListener}가 호출한다.
     * 투수 교체 시에도 재호출된다.</p>
     */
    public void drawCardHand(String matchSessionId) {
        // 초기 드로우된 카드 리스트(id)
        List<Long> drawnIds = cardHandDrawExecutor.execute(matchSessionId);
        // 초기 드로우된 카드의 정보 리스트(dto)
        List<CardInfo> cardInfos = pitchCardReader.getPitchCardDetails(drawnIds);

        messagingTemplate.convertAndSend(GAME_TOPIC + matchSessionId,
                new CardHandEvent(cardInfos));
    }

    /**
     * drawCardHand 페이즈의 두 번째 단계 — 멀리건(교체 확정).
     *
     * <p>멀리건 확정 후 {@link GameProgressService#ensureGameStarted}로 경기 진행을 초기화한다.
     * 이후 {@link GameTurnService}에서 투구·타격 턴이 시작된다.</p>
     *
     * @param request 교체할 카드 ID 목록 (빈 리스트 = 교체 없이 확정)
     */
    public void processMulligan(String matchSessionId, Long userId, MulliganRequest request) {
        // 카드 교체는 1회만 가능(검증 로직)
        mulliganValidator.validateMulliganAllowed(matchInfoReader.isMulliganDone(matchSessionId));

        // 초기 드로우 카드 리스트(id)
        List<Long> currentHand = matchInfoReader.getPitcherCardHand(matchSessionId);
        // 교체 요청된 카드 리스트(id)
        List<Long> cardIdsToSwap = request.cardIdsToSwap();

        // 최종 결정된 카드 리스트(id)
        List<Long> finalHand;
        // 교체 요청된 카드가 없는 경우(null, isEmpty)
        if (cardIdsToSwap == null || cardIdsToSwap.isEmpty()) {
            // 교체 없이 확정되는 경우
            mulliganExecutor.confirm(matchSessionId);
            // 현재 카드를 최종 카드로 일치
            finalHand = currentHand;
        } else {
            // 교체 요청되는 카드에 대한 유효값 검증
            mulliganValidator.validate(currentHand, cardIdsToSwap);
            // 최종적으로 카드 리스트 확정
            finalHand = mulliganExecutor.execute(matchSessionId, cardIdsToSwap);
        }

        // 최종 결정된 카드 리스트 정보 리스트(dto)
        List<CardInfo> cardInfos = pitchCardReader.getPitchCardDetails(finalHand);

        messagingTemplate.convertAndSend(GAME_TOPIC + matchSessionId,
                new CardHandEvent(cardInfos));

        // 경기 진행 도중 카드 교체 작업이 다시 수행되더라도 경기 내용 초기화 x를 보장하는 메서드
        gameProgressService.ensureGameStarted(matchSessionId, matchInfoReader.getGameMode(matchSessionId));
    }
}
