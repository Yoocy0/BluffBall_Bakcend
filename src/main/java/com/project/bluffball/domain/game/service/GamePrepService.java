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
 */
@Service
@RequiredArgsConstructor
public class GamePrepService {

    private final SetupNumberValidator setupNumberValidator;
    private final SetupNumberExecutor setupNumberExecutor;
    private final MulliganValidator mulliganValidator;
    private final MulliganExecutor mulliganExecutor;
    private final CardHandDrawExecutor cardHandDrawExecutor;
    private final MatchInfoReader matchInfoReader;
    private final PitchCardReader pitchCardReader;
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
        setupNumberValidator.validate(request);
        setupNumberExecutor.save(matchSessionId, userId, request);
        eventPublisher.publishEvent(new SetupNumbersSubmittedEvent(matchSessionId));
    }

    /**
     * 초기 카드 패를 뽑아 MatchInfo에 저장하고 투수에게 CardHandEvent를 전송한다.
     *
     * <p>양측 setup-numbers 완료 시 {@link com.project.bluffball.domain.game.service.listener.GamePhaseListener}가 호출한다.
     * 투수 교체 시에도 재호출된다.</p>
     */
    public void drawCardHand(String matchSessionId) {
        List<Long> drawnIds = cardHandDrawExecutor.execute(matchSessionId);
        List<CardInfo> cardInfos = pitchCardReader.getPitchCardDetails(drawnIds);
        messagingTemplate.convertAndSend(GAME_TOPIC + matchSessionId,
                CardHandEvent.builder().cards(cardInfos).build());
    }

    /**
     * drawCardHand 페이즈의 두 번째 단계 — 멀리건(교체 확정).
     *
     * <p>교체할 카드가 없으면 현재 패를 그대로 확정하고,
     * 있으면 Executor에 위임하여 교체 후 확정한다.
     * 어느 경우든 최종 확정된 패를 {@code /topic/game/{matchSessionId}}로 전송한다.</p>
     *
     * @param request 교체할 카드 ID 목록 (빈 리스트 = 교체 없이 확정)
     */
    public void processMulligan(String matchSessionId, Long userId, MulliganRequest request) {
        if (matchInfoReader.isMulliganDone(matchSessionId)) {
            throw new IllegalStateException("멀리건은 투수 등판 당 1회만 가능합니다.");
        }

        List<Long> currentHand = matchInfoReader.getPitcherCardHand(matchSessionId);
        List<Long> cardIdsToSwap = request.getCardIdsToSwap();

        List<Long> finalHand;
        if (cardIdsToSwap == null || cardIdsToSwap.isEmpty()) {
            mulliganExecutor.confirm(matchSessionId);
            finalHand = currentHand;
        } else {
            mulliganValidator.validate(currentHand, cardIdsToSwap);
            finalHand = mulliganExecutor.execute(matchSessionId, cardIdsToSwap);
        }

        List<CardInfo> cardInfos = pitchCardReader.getPitchCardDetails(finalHand);
        messagingTemplate.convertAndSend(GAME_TOPIC + matchSessionId,
                CardHandEvent.builder().cards(cardInfos).build());
    }
}
