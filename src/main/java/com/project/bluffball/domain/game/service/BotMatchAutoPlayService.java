package com.project.bluffball.domain.game.service;

import com.project.bluffball.domain.game.dto.request.BatterCardSelectRequest;
import com.project.bluffball.domain.game.dto.request.MulliganRequest;
import com.project.bluffball.domain.game.dto.request.PitcherCardSelectRequest;
import com.project.bluffball.domain.game.dto.request.SetupNumberRequest;
import com.project.bluffball.domain.game.dto.response.CardInfo;
import com.project.bluffball.domain.game.enums.SetupKind;
import com.project.bluffball.domain.game.enums.Timing;
import com.project.bluffball.domain.game.service.usecase.reader.CoordinateCardReader;
import com.project.bluffball.domain.game.service.usecase.reader.GameProgressReader;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.game.service.usecase.reader.PitchCardReader;
import com.project.bluffball.domain.game.service.usecase.reader.TurnResultSessionReader;
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
 * 프로덕션 봇 매치 자동 플레이 루프 (skeleton).
 *
 * <p>매치가 셋업·멀리건에서 멈추지 않도록 최소 동작을 수행한다.
 * 투수/타자 AI는 아직 없으며, 안전한 더미 선택만 한다.</p>
 *
 * <p>TODO: {@code gametest}의 Pitcher/Batter decision policy를 공통화해 플러그인으로 교체한다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BotMatchAutoPlayService {

    /** 폴링 주기 */
    private static final long TICK_MS = 1_000L;

    /** 봇 행동 최소 간격 */
    private static final long MIN_ACTION_GAP_MS = 3_000L;

    /** 더미 투구 시작 좌표 (중앙 근처) */
    private static final int STUB_START_COORDINATE = 13;

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

    /** 구종 Reader */
    private final PitchCardReader pitchCardReader;

    /** 좌표 카드 Reader */
    private final CoordinateCardReader coordinateCardReader;

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

    /**
     * 봇 자동 플레이를 시작한다. 이미 실행 중이면 재시작한다.
     *
     * @param matchSessionId 매치 세션 ID
     * @param botUserId      봇 유저 ID
     */
    public void start(String matchSessionId, Long botUserId) {
        stop(matchSessionId);
        lastActionAtMs.remove(matchSessionId);

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
    }

    /**
     * 한 틱: 셋업 → 멀리건 → (초기화 후) 투수/타자 더미 행동.
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
                actAsPitcherStub(matchSessionId, botUserId);
                lastActionAtMs.put(matchSessionId, System.currentTimeMillis());
                return;
            }

            if (botUserId.equals(batterUserId) && pitcherDone && !batterDone) {
                actAsBatterStub(matchSessionId, botUserId);
                lastActionAtMs.put(matchSessionId, System.currentTimeMillis());
            }
        } catch (Exception e) {
            log.warn("bot auto-play tick failed matchSessionId={}: {}", matchSessionId, e.toString());
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
     * 투수 역할 더미 투구 — 패 첫 장 + 고정 시작 좌표.
     *
     * <p>TODO: 난이도별 PitcherBotDecisionPolicy 연결.</p>
     *
     * @param matchSessionId 매치
     * @param pitcherUserId  투수(봇)
     */
    private void actAsPitcherStub(String matchSessionId, Long pitcherUserId) {
        List<Long> handIds = matchInfoReader.getPitcherCardHand(matchSessionId);
        if (handIds.isEmpty()) {
            handIds = matchInfoReader.getPlayerCardHand(matchSessionId, pitcherUserId);
        }
        if (handIds.isEmpty()) {
            return;
        }
        Long pitchCardId = handIds.get(0);
        Long coordinateCardId = coordinateCardReader.getCoordinateCardId(STUB_START_COORDINATE);
        gameTurnService.pitcherSelectCard(
                matchSessionId,
                pitcherUserId,
                new PitcherCardSelectRequest(pitchCardId, coordinateCardId));

        List<CardInfo> details = pitchCardReader.getPitchCardDetails(List.of(pitchCardId));
        String pitchName = details.isEmpty() ? String.valueOf(pitchCardId) : details.get(0).name();
        log.info("bot pitcher stub threw matchSessionId={} pitch={} start={}",
                matchSessionId, pitchName, STUB_START_COORDINATE);
    }

    /**
     * 타자 역할 더미 타격 — 시작 좌표에 NORMAL 스윙.
     *
     * <p>TODO: 난이도별 BatterBotDecisionPolicy 연결.</p>
     *
     * @param matchSessionId 매치
     * @param batterUserId   타자(봇)
     */
    private void actAsBatterStub(String matchSessionId, Long batterUserId) {
        int startCoord = turnResultSessionReader.findCurrentStartCoordinateNumber(matchSessionId)
                .orElse(STUB_START_COORDINATE);
        gameTurnService.batterSelectCard(
                matchSessionId,
                batterUserId,
                new BatterCardSelectRequest(1.5, startCoord, Timing.NORMAL));
        log.info("bot batter stub swung matchSessionId={} coord={}", matchSessionId, startCoord);
    }

    /**
     * 스케줄러를 종료한다.
     */
    @PreDestroy
    void shutdown() {
        running.values().forEach(f -> f.cancel(false));
        running.clear();
        lastActionAtMs.clear();
        scheduler.shutdownNow();
    }
}
