package com.project.bluffball.domain.game.service;

import com.project.bluffball.domain.game.config.GameModeRule;
import com.project.bluffball.domain.game.dto.request.LeagueSubstitutePitcherRequest;
import com.project.bluffball.domain.game.dto.request.MulliganRequest;
import com.project.bluffball.domain.game.dto.request.SetupNumberRequest;
import com.project.bluffball.domain.game.dto.response.CardHandEvent;
import com.project.bluffball.domain.game.dto.response.CardInfo;
import com.project.bluffball.domain.game.dto.response.PitcherSubstitutedEvent;
import com.project.bluffball.domain.game.dto.response.RoleChangedEvent;
import com.project.bluffball.domain.game.enums.SetupKind;
import com.project.bluffball.domain.game.event.SetupNumbersSubmittedEvent;
import com.project.bluffball.domain.game.service.usecase.executor.CardHandDrawExecutor;
import com.project.bluffball.domain.game.service.usecase.executor.HalfInningRoleSwapExecutor;
import com.project.bluffball.domain.game.service.usecase.executor.LeaguePitcherSubstituteExecutor;
import com.project.bluffball.domain.game.service.usecase.executor.MulliganExecutor;
import com.project.bluffball.domain.game.service.usecase.executor.SetupNumberExecutor;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.game.service.usecase.reader.PitchCardReader;
import com.project.bluffball.domain.game.service.usecase.validator.LeaguePitcherSubstituteValidator;
import com.project.bluffball.domain.game.service.usecase.validator.MulliganValidator;
import com.project.bluffball.domain.game.service.usecase.validator.SetupNumberValidator;
import com.project.bluffball.domain.user.record.enums.GameMode;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 인게임 준비 단계 서비스.
 *
 * <p>쇼다운: 셋업 → 드로우·멀리건 → 투구.
 * 리그: 역할별 셋업 → (멀리건 없음) → 투구.</p>
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
    private final LeaguePitcherSubstituteValidator leaguePitcherSubstituteValidator;
    private final LeaguePitcherSubstituteExecutor leaguePitcherSubstituteExecutor;
    private final MatchInfoReader matchInfoReader;
    private final PitchCardReader pitchCardReader;
    private final GameProgressService gameProgressService;
    private final GameModeRule gameModeRule;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;

    private static final String GAME_TOPIC = "/topic/game/";

    /**
     * 블러핑 숫자를 저장한다.
     *
     * <p>쇼다운은 FULL, 리그는 현재 필요한 PITCHER/BATTER만 받는다.</p>
     *
     * @param matchSessionId 매치 세션 ID
     * @param userId 유저 ID
     * @param request 블러핑 숫자
     */
    public void setupNumbers(String matchSessionId, Long userId, SetupNumberRequest request) {
        SetupKind requiredKind = matchInfoReader.getRequiredSetupKind(matchSessionId, userId);
        if (requiredKind == null) {
            throw new BadRequestException(
                    ErrorCode.GAME_INVALID_SETUP_NUMBERS,
                    "이미 필요한 셋업이 완료되었습니다. userId=" + userId);
        }
        log.info("setup-numbers 저장 matchSessionId={} userId={} kind={}",
                matchSessionId, userId, requiredKind);
        setupNumberValidator.validate(request, requiredKind);
        setupNumberExecutor.save(matchSessionId, userId, requiredKind, request);
        eventPublisher.publishEvent(new SetupNumbersSubmittedEvent(matchSessionId));
    }

    /**
     * 리그 셋업 완료 후 경기를 시작한다. (멀리건 없음)
     *
     * @param matchSessionId 매치 세션 ID
     */
    public void beginLeaguePlayIfReady(String matchSessionId) {
        if (!matchInfoReader.isSetupNumbersComplete(matchSessionId)) {
            return;
        }
        GameMode gameMode = matchInfoReader.getGameMode(matchSessionId);
        log.info("리그 셋업 완료 — 경기 시작 matchSessionId={} mode={}", matchSessionId, gameMode);
        gameProgressService.ensureGameStarted(matchSessionId, gameMode);
    }

    /**
     * 참가자 전원에게 구종 패를 드로우한다. (쇼다운 전용)
     *
     * @param matchSessionId 매치 세션 ID
     */
    public void drawCardHand(String matchSessionId) {
        cardHandDrawExecutor.executeForAllParticipants(matchSessionId);
        publishDrawEvents(matchSessionId);
    }

    /**
     * 공수 교대 시 역할만 교환 — 멀리건·카드 패는 게임 시작 시 확정된 것을 유지한다.
     *
     * @param matchSessionId 매치 세션 ID
     */
    public void beginHalfInningRoleSwap(String matchSessionId) {
        log.info("공수 교대 역할 전환 matchSessionId={}", matchSessionId);
        Long pitcherUserId = halfInningRoleSwapExecutor.swap(matchSessionId);
        messagingTemplate.convertAndSend(GAME_TOPIC + matchSessionId, new RoleChangedEvent(pitcherUserId));
    }

    /**
     * 드로우 이벤트를 참가자별로 전송한다.
     *
     * @param matchSessionId 매치 세션 ID
     */
    private void publishDrawEvents(String matchSessionId) {
        Long pitcherUserId = matchInfoReader.getPitcherUserId(matchSessionId);
        for (Long userId : matchInfoReader.getParticipantUserIds(matchSessionId)) {
            List<Long> handIds = matchInfoReader.getPlayerCardHand(matchSessionId, userId);
            List<CardInfo> cardInfos = pitchCardReader.getPitchCardDetails(handIds);
            publishCardHandEvent(matchSessionId, cardInfos, pitcherUserId, userId, false, false);
        }
    }

    /**
     * 카드 패 이벤트를 전송한다.
     *
     * @param matchSessionId 매치 세션 ID
     * @param cardInfos 카드 정보
     * @param pitcherUserId 투수 ID
     * @param targetUserId 대상 유저
     * @param allMulliganReady 전원 멀리건 완료
     * @param fromMulligan 멀리건 응답 여부
     */
    private void publishCardHandEvent(String matchSessionId,
                                      List<CardInfo> cardInfos,
                                      Long pitcherUserId,
                                      Long targetUserId,
                                      boolean allMulliganReady,
                                      boolean fromMulligan) {
        messagingTemplate.convertAndSend(GAME_TOPIC + matchSessionId,
                new CardHandEvent(cardInfos, pitcherUserId, targetUserId, allMulliganReady, fromMulligan));
    }

    /**
     * 리그 투수를 교체한다.
     *
     * <p>신임 투수는 투수 셋업 재제출, 강판 투수는 타순 슬롯을 이어받아 타자 셋업 재제출이 필요하다.</p>
     *
     * @param matchSessionId 매치 세션 ID
     * @param requestUserId 요청 유저(현재 투수)
     * @param request 교체 요청
     */
    public void substituteLeaguePitcher(
            String matchSessionId,
            Long requestUserId,
            LeagueSubstitutePitcherRequest request) {
        GameMode gameMode = matchInfoReader.getGameMode(matchSessionId);
        int maxSubs = gameModeRule.getMaxPitcherSubstitutions(gameMode);
        leaguePitcherSubstituteValidator.validateSubstitutionSupported(maxSubs);
        leaguePitcherSubstituteValidator.validateLimit(
                matchInfoReader.getPitcherSubstitutionCount(matchSessionId), maxSubs);

        Long currentPitcher = matchInfoReader.getPitcherUserId(matchSessionId);
        leaguePitcherSubstituteValidator.validateRequesterIsPitcher(requestUserId, currentPitcher);

        List<Long> defendingRoster = resolveDefendingRoster(matchSessionId, currentPitcher);
        List<Long> defendingBattingOrder = resolveDefendingBattingOrder(matchSessionId, currentPitcher);
        leaguePitcherSubstituteValidator.validateNewPitcher(
                request.newPitcherUserId(),
                currentPitcher,
                defendingRoster,
                defendingBattingOrder,
                matchInfoReader.getUsedAsPitcherIds(matchSessionId));

        Long dropCardId = matchInfoReader.getPlayerDropCard(matchSessionId, request.newPitcherUserId());
        leaguePitcherSubstituteValidator.validateDropCardPresent(dropCardId);

        Long newPitcherId = leaguePitcherSubstituteExecutor.execute(
                matchSessionId, currentPitcher, request.newPitcherUserId(), dropCardId);

        log.info("리그 투수 교체 matchSessionId={} old={} new={}",
                matchSessionId, currentPitcher, newPitcherId);

        messagingTemplate.convertAndSend(
                GAME_TOPIC + matchSessionId,
                new PitcherSubstitutedEvent(newPitcherId, currentPitcher, newPitcherId));
    }

    /**
     * 현재 투수가 속한 수비 로스터를 반환한다.
     *
     * @param matchSessionId 매치 세션 ID
     * @param pitcherUserId 현재 투수
     * @return 수비 로스터
     */
    private List<Long> resolveDefendingRoster(String matchSessionId, Long pitcherUserId) {
        List<Long> homeRoster = matchInfoReader.getHomeRosterUserIds(matchSessionId);
        if (homeRoster.contains(pitcherUserId)) {
            return homeRoster;
        }
        return matchInfoReader.getAwayRosterUserIds(matchSessionId);
    }

    private List<Long> resolveDefendingBattingOrder(String matchSessionId, Long pitcherUserId) {
        List<Long> homeRoster = matchInfoReader.getHomeRosterUserIds(matchSessionId);
        if (homeRoster.contains(pitcherUserId)) {
            return matchInfoReader.getHomeBattingOrderUserIds(matchSessionId);
        }
        return matchInfoReader.getAwayBattingOrderUserIds(matchSessionId);
    }

    /**
     * 멀리건을 처리한다. (쇼다운 전용)
     *
     * @param matchSessionId 매치 세션 ID
     * @param userId 유저 ID
     * @param request 멀리건 요청
     */
    public void processMulligan(String matchSessionId, Long userId, MulliganRequest request) {
        mulliganValidator.validateInGameMulliganSupported(matchInfoReader.getGameMode(matchSessionId));
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
