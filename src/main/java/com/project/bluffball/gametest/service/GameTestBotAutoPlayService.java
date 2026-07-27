package com.project.bluffball.gametest.service;

import com.project.bluffball.domain.game.dto.progress.GameProgressApplyResult;
import com.project.bluffball.domain.game.dto.request.BatterCardSelectRequest;
import com.project.bluffball.domain.game.dto.request.PitcherCardSelectRequest;
import com.project.bluffball.domain.game.dto.request.SetupNumberRequest;
import com.project.bluffball.domain.game.dto.response.CardInfo;
import com.project.bluffball.domain.game.dto.response.GameStateSnapshot;
import com.project.bluffball.domain.game.enums.SetupKind;
import com.project.bluffball.domain.game.service.GamePrepService;
import com.project.bluffball.domain.game.service.GameTurnService;
import com.project.bluffball.domain.game.service.usecase.reader.CoordinateCardReader;
import com.project.bluffball.domain.game.service.usecase.reader.GameProgressReader;
import com.project.bluffball.domain.game.service.usecase.reader.GameStateReader;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.game.service.usecase.reader.PitchCardReader;
import com.project.bluffball.domain.game.service.usecase.reader.TurnResultSessionReader;
import com.project.bluffball.gametest.bot.BatterBotDecisionPolicy;
import com.project.bluffball.gametest.bot.BatterBotPitchMemory;
import com.project.bluffball.gametest.bot.BatterBotSwingDecision;
import com.project.bluffball.gametest.bot.PitcherBotDecisionPolicy;
import com.project.bluffball.gametest.bot.PitcherBotThrowDecision;
import com.project.bluffball.gametest.dto.StartBotAutoPlayRequest;
import com.project.bluffball.gametest.dto.StartBotAutoPlayResponse;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 테스트용 봇 자동 플레이 루프.
 *
 * <p>Redis 상태를 500ms마다 폴링해 봇의 셋업·투구·타격을 수행한다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameTestBotAutoPlayService {

    private static final long TICK_MS = 500L;

    private final GamePrepService gamePrepService;
    private final GameTurnService gameTurnService;
    private final MatchInfoReader matchInfoReader;
    private final GameProgressReader gameProgressReader;
    private final GameStateReader gameStateReader;
    private final TurnResultSessionReader turnResultSessionReader;
    private final PitchCardReader pitchCardReader;
    private final CoordinateCardReader coordinateCardReader;
    private final PitcherBotDecisionPolicy pitcherBotDecisionPolicy;
    private final BatterBotDecisionPolicy batterBotDecisionPolicy;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "game-test-bot-autoplay");
                t.setDaemon(true);
                return t;
            });

    /** matchSessionId → 실행 중 future */
    private final Map<String, ScheduledFuture<?>> running = new ConcurrentHashMap<>();

    /** matchSessionId → 구종 기억 (타자 봇 공용) */
    private final Map<String, BatterBotPitchMemory> pitchMemories = new ConcurrentHashMap<>();

    /**
     * 봇 자동 플레이를 시작한다. 이미 돌고 있으면 재시작한다.
     *
     * @param request 매치·봇 ID
     * @return 시작 결과
     */
    public StartBotAutoPlayResponse start(StartBotAutoPlayRequest request) {
        String matchSessionId = request.matchSessionId();
        Set<Long> botUserIds = Set.copyOf(request.botUserIds());

        stop(matchSessionId);
        pitchMemories.put(matchSessionId, new BatterBotPitchMemory());

        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(
                () -> tick(matchSessionId, botUserIds),
                0,
                TICK_MS,
                TimeUnit.MILLISECONDS);
        running.put(matchSessionId, future);

        log.info("[game-test] bot auto-play start matchSessionId={} bots={}",
                matchSessionId, botUserIds.size());
        return new StartBotAutoPlayResponse(matchSessionId, botUserIds.size(), true);
    }

    /**
     * 해당 매치 자동 플레이를 중지한다.
     *
     * @param matchSessionId 매치 세션
     */
    public void stop(String matchSessionId) {
        ScheduledFuture<?> future = running.remove(matchSessionId);
        if (future != null) {
            future.cancel(false);
        }
        pitchMemories.remove(matchSessionId);
    }

    /**
     * 한 틱: 셋업 → 투수 → 타자.
     *
     * @param matchSessionId 매치
     * @param botUserIds 봇 집합
     */
    private void tick(String matchSessionId, Set<Long> botUserIds) {
        try {
            if (gameProgressReader.isGameOver(matchSessionId)) {
                log.info("[game-test] bot auto-play stop (game over) matchSessionId={}", matchSessionId);
                stop(matchSessionId);
                return;
            }

            submitSetups(matchSessionId, botUserIds);

            if (!matchInfoReader.isSetupNumbersComplete(matchSessionId)
                    || !gameProgressReader.isInitialized(matchSessionId)) {
                return;
            }

            Long pitcherUserId = matchInfoReader.getPitcherUserId(matchSessionId);
            Long batterUserId = matchInfoReader.getCurrentBatterUserId(matchSessionId);

            if (pitcherUserId != null
                    && botUserIds.contains(pitcherUserId)
                    && !turnResultSessionReader.isPitcherSelectionComplete(matchSessionId)) {
                actAsPitcher(matchSessionId, pitcherUserId);
                return;
            }

            if (batterUserId != null
                    && botUserIds.contains(batterUserId)
                    && turnResultSessionReader.isPitcherSelectionComplete(matchSessionId)
                    && !turnResultSessionReader.isBatterSelectionComplete(matchSessionId)) {
                actAsBatter(matchSessionId, batterUserId);
            }
        } catch (Exception e) {
            log.warn("[game-test] bot auto-play tick failed matchSessionId={}: {}",
                    matchSessionId, e.getMessage());
        }
    }

    /**
     * 봇들에게 필요한 셋업을 제출한다.
     *
     * @param matchSessionId 매치
     * @param botUserIds 봇
     */
    private void submitSetups(String matchSessionId, Set<Long> botUserIds) {
        for (Long userId : botUserIds) {
            SetupKind kind = matchInfoReader.getRequiredSetupKind(matchSessionId, userId);
            if (kind == null) {
                continue;
            }
            if (kind == SetupKind.PITCHER) {
                gamePrepService.setupNumbers(
                        matchSessionId,
                        userId,
                        new SetupNumberRequest(
                                List.of(1, 2, 3, 4, 5),
                                List.of(6),
                                null,
                                null));
            } else if (kind == SetupKind.BATTER) {
                gamePrepService.setupNumbers(
                        matchSessionId,
                        userId,
                        new SetupNumberRequest(
                                null,
                                null,
                                List.of(7),
                                List.of(8)));
            }
        }
    }

    /**
     * 투수 봇 투구.
     *
     * @param matchSessionId 매치
     * @param pitcherUserId 투수
     */
    private void actAsPitcher(String matchSessionId, Long pitcherUserId) {
        List<Long> handIds = matchInfoReader.getPitcherCardHand(matchSessionId);
        if (handIds.isEmpty()) {
            handIds = matchInfoReader.getPlayerCardHand(matchSessionId, pitcherUserId);
        }
        List<CardInfo> hand = pitchCardReader.getPitchCardDetails(handIds);
        GameStateSnapshot snap = gameStateReader.getSnapshot(matchSessionId);

        PitcherBotThrowDecision decision =
                pitcherBotDecisionPolicy.decide(hand, snap.balls(), snap.strikes());
        Long coordinateCardId =
                coordinateCardReader.getCoordinateCardId(decision.startCoordinateNumber());

        gameTurnService.pitcherSelectCard(
                matchSessionId,
                pitcherUserId,
                new PitcherCardSelectRequest(decision.pitchCardId(), coordinateCardId));

        log.debug("[game-test] pitcher-bot {} intent={} pitch={} start={}",
                pitcherUserId, decision.intent(), decision.pitchName(), decision.startCoordinateNumber());
    }

    /**
     * 타자 봇 타격.
     *
     * @param matchSessionId 매치
     * @param batterUserId 타자
     */
    private void actAsBatter(String matchSessionId, Long batterUserId) {
        int startCoord = turnResultSessionReader.findCurrentStartCoordinateNumber(matchSessionId)
                .orElseThrow(() -> new IllegalStateException("start coordinate missing"));
        Long pitchCardId = turnResultSessionReader.findCurrentSelectedPitchCardId(matchSessionId)
                .orElse(null);
        String pitchName = pitchCardId != null ? pitchCardReader.getPitchCardName(pitchCardId) : null;

        List<CardInfo> catalog = pitchCardReader.getPitchCardDetails(pitchCardReader.findAllIds());
        BatterBotPitchMemory memory =
                pitchMemories.computeIfAbsent(matchSessionId, id -> new BatterBotPitchMemory());

        BatterBotSwingDecision decision =
                batterBotDecisionPolicy.decide(startCoord, catalog, memory);

        BatterCardSelectRequest request = new BatterCardSelectRequest(
                1.5,
                decision.batterCoordinateNumber(),
                decision.timing());

        GameProgressApplyResult result =
                gameTurnService.batterSelectCard(matchSessionId, batterUserId, request);

        if (pitchName != null && result.turnResult() != null) {
            memory.remember(pitchName, result.turnResult());
        }

        log.debug("[game-test] batter-bot {} swing={} coord={} assumed={} result={}",
                batterUserId,
                decision.swing(),
                decision.batterCoordinateNumber(),
                decision.assumedPitchName(),
                result.turnResult());

        if (result.gameOver()) {
            stop(matchSessionId);
        }
    }

    /**
     * 스케줄러를 종료한다.
     */
    @PreDestroy
    void shutdown() {
        running.values().forEach(f -> f.cancel(false));
        running.clear();
        pitchMemories.clear();
        scheduler.shutdownNow();
    }
}
