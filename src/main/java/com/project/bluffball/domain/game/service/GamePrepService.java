package com.project.bluffball.domain.game.service;

import com.project.bluffball.domain.game.dto.request.MulliganRequest;
import com.project.bluffball.domain.game.dto.request.SetupNumberRequest;
import com.project.bluffball.domain.game.dto.response.CardHandEvent;
import com.project.bluffball.domain.game.dto.response.CardInfo;
import com.project.bluffball.domain.game.dto.response.RoleChangedEvent;
import com.project.bluffball.domain.game.event.SetupNumbersSubmittedEvent;
import com.project.bluffball.domain.game.service.usecase.executor.CardHandDrawExecutor;
import com.project.bluffball.domain.game.service.usecase.executor.HalfInningRoleSwapExecutor;
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
 * <p>참가자 전원이 멀리건을 완료해야 투구·타격 턴이 시작된다.</p>
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
    private final HalfInningRoleSwapExecutor halfInningRoleSwapExecutor;
    private final MatchInfoReader matchInfoReader;
    private final PitchCardReader pitchCardReader;
    private final GameProgressService gameProgressService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;

    private static final String GAME_TOPIC = "/topic/game/";

    public void setupNumbers(String matchSessionId, Long userId, SetupNumberRequest request) {
        log.info("setup-numbers 저장 matchSessionId={} userId={}", matchSessionId, userId);
        setupNumberValidator.validate(request);
        setupNumberExecutor.save(matchInfoReader.getById(matchSessionId), userId, request);
        eventPublisher.publishEvent(new SetupNumbersSubmittedEvent(matchSessionId));
    }

    public void drawCardHand(String matchSessionId) {
        cardHandDrawExecutor.executeForAllParticipants(matchSessionId);
        publishDrawEvents(matchSessionId);
    }

    /**
     * 공수 교대 시 역할만 교환 — 멀리건·카드 패는 게임 시작 시 확정된 것을 유지한다.
     */
    public void beginHalfInningRoleSwap(String matchSessionId) {
        log.info("공수 교대 역할 전환 matchSessionId={}", matchSessionId);
        halfInningRoleSwapExecutor.swapForSingleMode(matchSessionId);
        Long pitcherUserId = matchInfoReader.getPitcherUserId(matchSessionId);
        messagingTemplate.convertAndSend(GAME_TOPIC + matchSessionId, new RoleChangedEvent(pitcherUserId));
    }

    private void publishDrawEvents(String matchSessionId) {
        Long pitcherUserId = matchInfoReader.getPitcherUserId(matchSessionId);
        for (Long userId : matchInfoReader.getParticipantUserIds(matchSessionId)) {
            List<Long> handIds = matchInfoReader.getPlayerCardHand(matchSessionId, userId);
            List<CardInfo> cardInfos = pitchCardReader.getPitchCardDetails(handIds);
            publishCardHandEvent(matchSessionId, cardInfos, pitcherUserId, userId, false, false);
        }
    }

    private void publishCardHandEvent(String matchSessionId,
                                      List<CardInfo> cardInfos,
                                      Long pitcherUserId,
                                      Long targetUserId,
                                      boolean allMulliganReady,
                                      boolean fromMulligan) {
        messagingTemplate.convertAndSend(GAME_TOPIC + matchSessionId,
                new CardHandEvent(cardInfos, pitcherUserId, targetUserId, allMulliganReady, fromMulligan));
    }

    public void processMulligan(String matchSessionId, Long userId, MulliganRequest request) {
        mulliganValidator.validateParticipant(matchInfoReader, matchSessionId, userId);
        mulliganValidator.validateMulliganAllowedForUser(
                matchInfoReader.isMulliganDoneForUser(matchSessionId, userId));

        List<Long> currentHand = matchInfoReader.getPlayerCardHand(matchSessionId, userId);
        List<Long> cardIdsToSwap = request.cardIdsToSwap();

        List<Long> finalHand;
        if (cardIdsToSwap == null || cardIdsToSwap.isEmpty()) {
            mulliganExecutor.confirm(matchSessionId, userId);
            finalHand = currentHand;
        } else {
            mulliganValidator.validate(currentHand, cardIdsToSwap);
            finalHand = mulliganExecutor.execute(matchSessionId, userId, cardIdsToSwap);
        }

        Long pitcherUserId = matchInfoReader.getPitcherUserId(matchSessionId);
        boolean allReady = matchInfoReader.isMulliganDone(matchSessionId);
        List<CardInfo> cardInfos = pitchCardReader.getPitchCardDetails(finalHand);

        publishCardHandEvent(matchSessionId, cardInfos, pitcherUserId, userId, allReady, true);

        if (allReady) {
            for (Long participantId : matchInfoReader.getParticipantUserIds(matchSessionId)) {
                if (participantId.equals(userId)) {
                    continue;
                }
                List<Long> hand = matchInfoReader.getPlayerCardHand(matchSessionId, participantId);
                publishCardHandEvent(
                        matchSessionId,
                        pitchCardReader.getPitchCardDetails(hand),
                        pitcherUserId,
                        participantId,
                        true,
                        true);
            }
            gameProgressService.ensureGameStarted(matchSessionId, matchInfoReader.getGameMode(matchSessionId));
        }
    }
}
