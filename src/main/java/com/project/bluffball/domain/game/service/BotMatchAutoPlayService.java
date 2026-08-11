package com.project.bluffball.domain.game.service;

import com.project.bluffball.domain.card.service.usecase.reader.UserPitchCardReader;
import com.project.bluffball.domain.game.bot.BatterBotDecisionPolicy;
import com.project.bluffball.domain.game.bot.BatterBotPitchMemory;
import com.project.bluffball.domain.game.bot.BatterBotSwingDecision;
import com.project.bluffball.domain.game.bot.PitcherBotDecisionPolicy;
import com.project.bluffball.domain.game.bot.PitcherBotThrowDecision;
import com.project.bluffball.domain.game.config.GameModeRule;
import com.project.bluffball.domain.game.dto.progress.GameProgressApplyResult;
import com.project.bluffball.domain.game.dto.progress.GameProgressSituation;
import com.project.bluffball.domain.game.dto.request.BatterCardSelectRequest;
import com.project.bluffball.domain.game.dto.request.MulliganRequest;
import com.project.bluffball.domain.game.dto.request.PitcherCardSelectRequest;
import com.project.bluffball.domain.game.dto.request.SetupNumberRequest;
import com.project.bluffball.domain.game.dto.response.CardInfo;
import com.project.bluffball.domain.game.enums.BotDifficulty;
import com.project.bluffball.domain.game.enums.SetupKind;
import com.project.bluffball.domain.game.enums.Timing;
import com.project.bluffball.domain.game.service.usecase.reader.CoordinateCardReader;
import com.project.bluffball.domain.game.service.usecase.reader.GameProgressReader;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.game.service.usecase.reader.PitchCardReader;
import com.project.bluffball.domain.game.service.usecase.reader.TurnResultSessionReader;
import com.project.bluffball.domain.user.record.enums.GameMode;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 프로덕션 봇 매치 자동 플레이 루프.
 *
 * <p>셋업·멀리건을 자동 처리하고, 투수·타자 모두 EASY/NORMAL/HARD
 * 정책으로 행동한다. 난이도 null만 stub이다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BotMatchAutoPlayService {

    /** 폴링 주기 */
    private static final long TICK_MS = 1_000L;

    /** 봇 행동 최소 간격 */
    private static final long MIN_ACTION_GAP_MS = 3_000L;

    /** 난이도 null stub·시작 좌표 폴백 (중앙) */
    private static final int STUB_START_COORDINATE = 13;

    /** 타자 봇 응답 시간(초) */
    private static final double BATTER_RESPONSE_TIME_SEC = 1.5;

    /** 준비·턴 서비스 */
    private final GamePrepService gamePrepService;

    /** 투수/타자 선택 서비스 */
    private final GameTurnService gameTurnService;

    /** 매치 정보 Reader */
    private final MatchInfoReader matchInfoReader;

    /** 경기 진행 Reader */
    private final GameProgressReader gameProgressReader;

    /** 턴 세션 Reader */
    private final TurnResultSessionReader turnResultSessionReader;

    /** 좌표 카드 Reader */
    private final CoordinateCardReader coordinateCardReader;

    /** 투수 실효 구종 Reader */
    private final UserPitchCardReader userPitchCardReader;

    /** 마스터 구종 카탈로그 Reader */
    private final PitchCardReader pitchCardReader;

    /** 모드별 핸드 장수 */
    private final GameModeRule gameModeRule;

    /** EASY/NORMAL/HARD 투수 판단 정책 */
    private final PitcherBotDecisionPolicy pitcherBotDecisionPolicy;

    /** EASY/NORMAL/HARD 타자 판단 정책 */
    private final BatterBotDecisionPolicy batterBotDecisionPolicy;

    /** 단일 스레드 스케줄러 */
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "bot-match-autoplay");
                t.setDaemon(true);
                return t;
            });

    /** matchSessionId → 실행 중 future */
    private final Map<String, ScheduledFuture<?>> running = new ConcurrentHashMap<>();

    /** matchSessionId → 마지막 봇 행동 시각 */
    private final Map<String, Long> lastActionAtMs = new ConcurrentHashMap<>();

    /** matchSessionId → 타자 구종 기억(마스터 ID) */
    private final Map<String, BatterBotPitchMemory> pitchMemories = new ConcurrentHashMap<>();

    /**
     * 봇 자동 플레이를 시작한다. 이미 실행 중이면 재시작한다.
     *
     * @param matchSessionId 매치 세션 ID
     * @param botUserId      봇 유저 ID
     */
    public void start(String matchSessionId, Long botUserId) {
        stop(matchSessionId);
        lastActionAtMs.remove(matchSessionId);
        pitchMemories.put(matchSessionId, new BatterBotPitchMemory());

        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(
                () -> tick(matchSessionId, botUserId),
                0,
                TICK_MS,
                TimeUnit.MILLISECONDS);
        running.put(matchSessionId, future);
        log.info("bot auto-play start matchSessionId={} botUserId={}", matchSessionId, botUserId);
    }

    /**
     * 해당 매치 자동 플레이를 중지한다.
     *
     * @param matchSessionId 매치 세션 ID
     */
    public void stop(String matchSessionId) {
        ScheduledFuture<?> future = running.remove(matchSessionId);
        if (future != null) {
            future.cancel(false);
        }
        lastActionAtMs.remove(matchSessionId);
        pitchMemories.remove(matchSessionId);
    }

    /**
     * 한 틱: 셋업 → 멀리건 → (초기화 후) 투수/타자 행동.
     *
     * @param matchSessionId 매치 세션
     * @param botUserId      봇
     */
    private void tick(String matchSessionId, Long botUserId) {
        try {
            if (gameProgressReader.isGameOver(matchSessionId)) {
                log.info("bot auto-play stop (game over) matchSessionId={}", matchSessionId);
                stop(matchSessionId);
                return;
            }

            submitSetupIfNeeded(matchSessionId, botUserId);
            confirmMulliganIfNeeded(matchSessionId, botUserId);

            if (!matchInfoReader.isSetupNumbersComplete(matchSessionId)
                    || !matchInfoReader.isMulliganDone(matchSessionId)
                    || !gameProgressReader.isInitialized(matchSessionId)) {
                return;
            }

            long now = System.currentTimeMillis();
            Long lastAct = lastActionAtMs.get(matchSessionId);
            if (lastAct != null && now - lastAct < MIN_ACTION_GAP_MS) {
                return;
            }

            Long pitcherUserId = matchInfoReader.getPitcherUserId(matchSessionId);
            Long batterUserId = matchInfoReader.getCurrentBatterUserId(matchSessionId);
            boolean pitcherDone = turnResultSessionReader.isPitcherSelectionComplete(matchSessionId);
            boolean batterDone = turnResultSessionReader.isBatterSelectionComplete(matchSessionId);

            if (botUserId.equals(pitcherUserId) && !pitcherDone) {
                actAsPitcher(matchSessionId, botUserId);
                lastActionAtMs.put(matchSessionId, System.currentTimeMillis());
                return;
            }

            if (botUserId.equals(batterUserId) && pitcherDone && !batterDone) {
                actAsBatter(matchSessionId, botUserId);
                lastActionAtMs.put(matchSessionId, System.currentTimeMillis());
            }
        } catch (Exception e) {
            log.warn("bot auto-play tick failed matchSessionId={}", matchSessionId, e);
        }
    }

    /**
     * 봇이 아직 셋업하지 않았다면 FULL 셋업을 제출한다.
     *
     * @param matchSessionId 매치
     * @param botUserId      봇
     */
    private void submitSetupIfNeeded(String matchSessionId, Long botUserId) {
        SetupKind kind = matchInfoReader.getRequiredSetupKind(matchSessionId, botUserId);
        if (kind == null) {
            return;
        }
        // Showdown은 FULL만 사용. 더미 숫자(겹침 없는 고정값).
        gamePrepService.setupNumbers(
                matchSessionId,
                botUserId,
                new SetupNumberRequest(
                        List.of(1, 2, 3, 4, 5),
                        List.of(6),
                        List.of(7),
                        List.of(8)));
        log.info("bot setup submitted matchSessionId={} botUserId={} kind={}",
                matchSessionId, botUserId, kind);
    }

    /**
     * 카드가 드로우된 뒤 봇 멀리건을 빈 교체로 확정한다.
     *
     * @param matchSessionId 매치
     * @param botUserId      봇
     */
    private void confirmMulliganIfNeeded(String matchSessionId, Long botUserId) {
        if (!matchInfoReader.isSetupNumbersComplete(matchSessionId)) {
            return;
        }
        if (matchInfoReader.isMulliganDoneForUser(matchSessionId, botUserId)) {
            return;
        }
        List<Long> hand = matchInfoReader.getPlayerCardHand(matchSessionId, botUserId);
        if (hand == null || hand.isEmpty()) {
            return;
        }
        gamePrepService.processMulligan(matchSessionId, botUserId, new MulliganRequest(List.of()));
        log.info("bot mulligan confirmed matchSessionId={} botUserId={}", matchSessionId, botUserId);
    }

    /**
     * 투수 역할 투구 — EASY/NORMAL/HARD는 정책, 난이도 null만 stub(첫 장 + 좌표 13).
     *
     * @param matchSessionId 매치
     * @param pitcherUserId  투수(봇)
     */
    private void actAsPitcher(String matchSessionId, Long pitcherUserId) {
        List<Long> handIds = matchInfoReader.getPitcherCardHand(matchSessionId);
        if (handIds.isEmpty()) {
            handIds = matchInfoReader.getPlayerCardHand(matchSessionId, pitcherUserId);
        }
        if (handIds.isEmpty()) {
            return;
        }

        BotDifficulty difficulty = matchInfoReader.getBotDifficulty(matchSessionId);
        // 난이도 미설정만 이전 stub 유지 — HARD는 확률 정책 사용
        if (difficulty == null) {
            actAsPitcherNullDifficultyStub(matchSessionId, pitcherUserId, handIds);
            return;
        }

        List<CardInfo> hand = userPitchCardReader.getEffectiveCardInfos(pitcherUserId, handIds);
        GameProgressSituation situation = gameProgressReader.getSituation(matchSessionId);
        PitcherBotThrowDecision decision = pitcherBotDecisionPolicy.decide(
                hand, situation.balls(), situation.strikes(), difficulty);

        Long coordinateCardId = coordinateCardReader.getCoordinateCardId(decision.startCoordinateNumber());
        gameTurnService.pitcherSelectCard(
                matchSessionId,
                pitcherUserId,
                new PitcherCardSelectRequest(decision.pitchCardId(), coordinateCardId));

        String pitchName = hand.stream()
                .filter(c -> c.cardId().equals(decision.pitchCardId()))
                .map(CardInfo::name)
                .findFirst()
                .orElse(String.valueOf(decision.pitchCardId()));
        log.info("bot pitcher threw matchSessionId={} difficulty={} pitch={} start={} count={}-{}",
                matchSessionId, difficulty, pitchName, decision.startCoordinateNumber(),
                situation.balls(), situation.strikes());
    }

    /**
     * 난이도 미설정 투수 stub — 패 첫 장 + 고정 시작 좌표 13.
     *
     * @param matchSessionId 매치
     * @param pitcherUserId  투수(봇)
     * @param handIds        핸드 카드 ID
     */
    private void actAsPitcherNullDifficultyStub(
            String matchSessionId, Long pitcherUserId, List<Long> handIds) {
        Long pitchCardId = handIds.get(0);
        Long coordinateCardId = coordinateCardReader.getCoordinateCardId(STUB_START_COORDINATE);
        gameTurnService.pitcherSelectCard(
                matchSessionId,
                pitcherUserId,
                new PitcherCardSelectRequest(pitchCardId, coordinateCardId));
        log.info("bot pitcher null-difficulty stub threw matchSessionId={} pitchCardId={} start={}",
                matchSessionId, pitchCardId, STUB_START_COORDINATE);
    }

    /**
     * 타자 역할 — EASY/NORMAL/HARD 정책. 난이도 null은 stub(시작 좌표 NORMAL 스윙).
     *
     * <p>선택 전 구종 ID는 기억용으로만 읽고 정책에는 넘기지 않는다.</p>
     *
     * @param matchSessionId 매치
     * @param batterUserId   타자(봇)
     */
    private void actAsBatter(String matchSessionId, Long batterUserId) {
        int startCoord = turnResultSessionReader.findCurrentStartCoordinateNumber(matchSessionId)
                .orElse(STUB_START_COORDINATE);

        BotDifficulty difficulty = matchInfoReader.getBotDifficulty(matchSessionId);
        if (difficulty == null) {
            actAsBatterNullDifficultyStub(matchSessionId, batterUserId, startCoord);
            return;
        }

        // 세션이 턴 반영 후 지워질 수 있어 선택 전에만 읽고, 정책에는 사용하지 않음
        Long selectedPitchCardId = turnResultSessionReader
                .findCurrentSelectedPitchCardId(matchSessionId)
                .orElse(null);

        List<CardInfo> catalog = pitchCardReader.getPitchCardDetails(pitchCardReader.findAllIds());
        BatterBotPitchMemory memory =
                pitchMemories.computeIfAbsent(matchSessionId, id -> new BatterBotPitchMemory());
        GameMode gameMode = matchInfoReader.getGameMode(matchSessionId);
        int maxKnown = gameModeRule.getHandSize(gameMode);
        List<CardInfo> pool = memory.considerationPool(catalog, maxKnown);

        GameProgressSituation situation = gameProgressReader.getSituation(matchSessionId);
        BatterBotSwingDecision decision = batterBotDecisionPolicy.decide(
                startCoord,
                situation.balls(),
                situation.strikes(),
                difficulty,
                pool,
                memory.isPassive());

        BatterCardSelectRequest request = decision.swing()
                ? new BatterCardSelectRequest(
                        BATTER_RESPONSE_TIME_SEC,
                        decision.batterCoordinateNumber(),
                        decision.timing())
                : new BatterCardSelectRequest(BATTER_RESPONSE_TIME_SEC, 0, Timing.NORMAL);

        GameProgressApplyResult result =
                gameTurnService.batterSelectCard(matchSessionId, batterUserId, request);

        if (selectedPitchCardId != null && result.turnResult() != null) {
            Long pitcherUserId = matchInfoReader.getPitcherUserId(matchSessionId);
            Long masterId = userPitchCardReader.resolveMasterCardId(pitcherUserId, selectedPitchCardId);
            memory.remember(masterId);
        }

        log.info(
                "bot batter acted matchSessionId={} difficulty={} swing={} coord={} assumed={} count={}-{} seen={}",
                matchSessionId,
                difficulty,
                decision.swing(),
                decision.batterCoordinateNumber(),
                decision.assumedPitchName(),
                situation.balls(),
                situation.strikes(),
                memory.size());
    }

    /**
     * 난이도 미설정 타자 stub — 시작 좌표에 NORMAL 스윙.
     *
     * @param matchSessionId 매치
     * @param batterUserId   타자
     * @param startCoord     시작 좌표
     */
    private void actAsBatterNullDifficultyStub(
            String matchSessionId, Long batterUserId, int startCoord) {
        gameTurnService.batterSelectCard(
                matchSessionId,
                batterUserId,
                new BatterCardSelectRequest(BATTER_RESPONSE_TIME_SEC, startCoord, Timing.NORMAL));
        log.info("bot batter null-difficulty stub swung matchSessionId={} coord={}",
                matchSessionId, startCoord);
    }

    /**
     * 스케줄러를 종료한다.
     */
    @PreDestroy
    void shutdown() {
        running.values().forEach(f -> f.cancel(false));
        running.clear();
        lastActionAtMs.clear();
        pitchMemories.clear();
        scheduler.shutdownNow();
    }
}
