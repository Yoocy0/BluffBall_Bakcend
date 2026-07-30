package com.project.bluffball.gametest.service;



import com.project.bluffball.domain.game.dto.progress.GameProgressApplyResult;

import com.project.bluffball.domain.game.dto.request.BatterCardSelectRequest;

import com.project.bluffball.domain.game.dto.request.PitcherCardSelectRequest;

import com.project.bluffball.domain.game.dto.request.SetupNumberRequest;

import com.project.bluffball.domain.game.dto.response.CardInfo;

import com.project.bluffball.domain.game.enums.SetupKind;

import com.project.bluffball.domain.game.enums.Timing;

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



import java.util.LinkedHashSet;

import java.util.List;

import java.util.Map;

import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

import java.util.concurrent.Executors;

import java.util.concurrent.ScheduledExecutorService;

import java.util.concurrent.ScheduledFuture;

import java.util.concurrent.TimeUnit;

import java.util.stream.Collectors;



/**

 * 테스트용 봇 자동 플레이 루프.

 *

 * <p>Redis 상태를 폴링해 봇의 셋업·투구·타격을 수행한다.</p>

 */

@Service

@RequiredArgsConstructor

@Slf4j

public class GameTestBotAutoPlayService {



    private static final long TICK_MS = 1_000L;

    /** 봇 행동 사이 최소 간격 — 사람이 화면을 읽고 따라갈 수 있게 */

    private static final long MIN_ACTION_GAP_MS = 4_000L;

    /** 셋업 완료 후 첫 투구까지 여유 (타자 대기 화면 진입 시간) */

    private static final long POST_SETUP_DELAY_MS = 5_000L;



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



    /** matchSessionId → 마지막 봇 행동 시각 */

    private final Map<String, Long> lastActionAtMs = new ConcurrentHashMap<>();



    /** matchSessionId → 첫 투구 허용 시각 (셋업 직후 딜레이) */

    private final Map<String, Long> firstPitchAllowedAtMs = new ConcurrentHashMap<>();



    /** matchSessionId → 최근 "대기 중" 로그 시각 (스팸 방지) */

    private final Map<String, Long> lastWaitLogAtMs = new ConcurrentHashMap<>();



    /**

     * 봇 자동 플레이를 시작한다. 이미 돌고 있으면 재시작한다.

     *

     * @param request 매치·봇 ID

     * @return 시작 결과

     */

    public StartBotAutoPlayResponse start(StartBotAutoPlayRequest request) {

        String matchSessionId = request.matchSessionId();

        Set<Long> botUserIds = normalizeIds(request.botUserIds());



        stop(matchSessionId);

        pitchMemories.put(matchSessionId, new BatterBotPitchMemory());

        lastActionAtMs.remove(matchSessionId);

        firstPitchAllowedAtMs.remove(matchSessionId);

        lastWaitLogAtMs.remove(matchSessionId);



        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(

                () -> tick(matchSessionId, botUserIds),

                0,

                TICK_MS,

                TimeUnit.MILLISECONDS);

        running.put(matchSessionId, future);



        log.info("[game-test] bot auto-play start matchSessionId={} bots={} ids={} gapMs={}",

                matchSessionId, botUserIds.size(), botUserIds, MIN_ACTION_GAP_MS);

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

        lastActionAtMs.remove(matchSessionId);

        firstPitchAllowedAtMs.remove(matchSessionId);

        lastWaitLogAtMs.remove(matchSessionId);

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



            try {

                submitSetups(matchSessionId, botUserIds);

            } catch (Exception e) {

                log.warn("[game-test] bot setup tick failed matchSessionId={}: {}",

                        matchSessionId, e.toString());

            }



            if (!matchInfoReader.isSetupNumbersComplete(matchSessionId)

                    || !gameProgressReader.isInitialized(matchSessionId)) {

                return;

            }



            long now = System.currentTimeMillis();

            firstPitchAllowedAtMs.putIfAbsent(matchSessionId, now + POST_SETUP_DELAY_MS);

            if (now < firstPitchAllowedAtMs.get(matchSessionId)) {

                return;

            }



            Long lastAct = lastActionAtMs.get(matchSessionId);

            if (lastAct != null && now - lastAct < MIN_ACTION_GAP_MS) {

                return;

            }



            Long pitcherUserId = normalizeId(matchInfoReader.getPitcherUserId(matchSessionId));

            Long batterUserId = normalizeId(matchInfoReader.getCurrentBatterUserId(matchSessionId));

            boolean pitcherDone = turnResultSessionReader.isPitcherSelectionComplete(matchSessionId);

            boolean batterDone = turnResultSessionReader.isBatterSelectionComplete(matchSessionId);



            if (pitcherUserId != null

                    && botUserIds.contains(pitcherUserId)

                    && !pitcherDone) {

                actAsPitcher(matchSessionId, pitcherUserId);

                lastActionAtMs.put(matchSessionId, System.currentTimeMillis());

                return;

            }



            if (batterUserId != null

                    && botUserIds.contains(batterUserId)

                    && pitcherDone

                    && !batterDone) {

                actAsBatter(matchSessionId, batterUserId);

                lastActionAtMs.put(matchSessionId, System.currentTimeMillis());

                return;

            }



            logWaiting(matchSessionId, pitcherUserId, batterUserId, botUserIds, pitcherDone, batterDone);

        } catch (Exception e) {

            log.warn("[game-test] bot auto-play tick failed matchSessionId={}: {}",

                    matchSessionId, e.toString(), e);

        }

    }



    private void logWaiting(

            String matchSessionId,

            Long pitcherUserId,

            Long batterUserId,

            Set<Long> botUserIds,

            boolean pitcherDone,

            boolean batterDone) {

        long now = System.currentTimeMillis();

        Long last = lastWaitLogAtMs.get(matchSessionId);

        if (last != null && now - last < 8_000L) {

            return;

        }

        lastWaitLogAtMs.put(matchSessionId, now);



        if (!pitcherDone) {

            if (pitcherUserId != null && !botUserIds.contains(pitcherUserId)) {

                log.info("[game-test] waiting human/non-bot pitcher match={} pitcher={}",

                        matchSessionId, pitcherUserId);

            }

            return;

        }

        if (!batterDone) {

            if (batterUserId != null && !botUserIds.contains(batterUserId)) {

                log.info("[game-test] waiting human/non-bot batter match={} batter={}",

                        matchSessionId, batterUserId);

            } else if (batterUserId != null) {

                log.info("[game-test] waiting batter window match={} batter={} pitcherDone={}",

                        matchSessionId, batterUserId, pitcherDone);

            }

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

        var snap = gameStateReader.getSnapshot(matchSessionId);



        PitcherBotThrowDecision decision =

                pitcherBotDecisionPolicy.decide(hand, snap.balls(), snap.strikes());

        Long coordinateCardId =

                coordinateCardReader.getCoordinateCardId(decision.startCoordinateNumber());



        gameTurnService.pitcherSelectCard(

                matchSessionId,

                pitcherUserId,

                new PitcherCardSelectRequest(decision.pitchCardId(), coordinateCardId));



        log.info("[game-test] pitcher-bot threw matchSessionId={} pitcher={} pitch={} start={}",

                matchSessionId, pitcherUserId, decision.pitchName(), decision.startCoordinateNumber());

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



        BatterCardSelectRequest request = decision.swing()

                ? new BatterCardSelectRequest(

                        1.5,

                        decision.batterCoordinateNumber(),

                        decision.timing())

                : new BatterCardSelectRequest(1.5, 0, Timing.NORMAL);



        GameProgressApplyResult result =

                gameTurnService.batterSelectCard(matchSessionId, batterUserId, request);



        if (pitchName != null && result.turnResult() != null) {

            memory.remember(pitchName, result.turnResult());

        }



        log.info("[game-test] batter-bot acted matchSessionId={} batter={} swing={} coord={} assumed={} result={}",

                matchSessionId,

                batterUserId,

                decision.swing(),

                decision.batterCoordinateNumber(),

                decision.assumedPitchName(),

                result.turnResult());



        if (result.gameOver()) {

            stop(matchSessionId);

        }

    }



    private static Set<Long> normalizeIds(List<Long> ids) {

        if (ids == null || ids.isEmpty()) {

            return Set.of();

        }

        return ids.stream()

                .map(GameTestBotAutoPlayService::normalizeId)

                .filter(id -> id != null)

                .collect(Collectors.toCollection(LinkedHashSet::new));

    }



    private static Long normalizeId(Number id) {

        return id == null ? null : id.longValue();

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


