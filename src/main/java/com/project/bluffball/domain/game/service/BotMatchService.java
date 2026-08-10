package com.project.bluffball.domain.game.service;

import com.project.bluffball.domain.game.bot.BotDeckCatalog;
import com.project.bluffball.domain.game.dto.request.BotMatchStartRequest;
import com.project.bluffball.domain.game.dto.response.BotMatchStartResponse;
import com.project.bluffball.domain.game.dto.response.MatchFoundEvent;
import com.project.bluffball.domain.game.enums.BotDifficulty;
import com.project.bluffball.domain.game.service.usecase.executor.BotUserPoolExecutor;
import com.project.bluffball.domain.game.service.usecase.executor.SingleMatchCreateExecutor;
import com.project.bluffball.domain.game.service.usecase.validator.BotMatchValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Showdown 봇 매치 서비스.
 *
 * <p>공개 PvP 큐를 사용하지 않고 {@link SingleMatchCreateExecutor}로 직접 생성한다.
 * 연습 전용(보상·래더 없음).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BotMatchService {

    private static final String USER_MATCH_TOPIC_PREFIX = "/topic/user/";

    /** 요청 값 검증 */
    private final BotMatchValidator botMatchValidator;

    /** 난이도별 재사용 봇 유저 */
    private final BotUserPoolExecutor botUserPoolExecutor;

    /** Showdown 매치 생성 */
    private final SingleMatchCreateExecutor singleMatchCreateExecutor;

    /** 큐 취소(매칭 전환 시) */
    private final MatchService matchService;

    /** 봇 자동 플레이 */
    private final BotMatchAutoPlayService botMatchAutoPlayService;

    /** 난이도별 덱 카탈로그 (현재는 placeholder) */
    private final BotDeckCatalog botDeckCatalog;

    /** 개인 매칭 알림 */
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Showdown 1v1 봇 매치를 시작한다.
     *
     * <p>사람은 초기 투수(홈), 봇은 초기 타자(어웨이)로 배정한다.
     * {@code fromMatchmaking=true}이면 대기 큐를 soft-cancel 한다.</p>
     *
     * @param humanUserId JWT 유저 ID
     * @param request     난이도·전환 여부
     * @return 매치 세션·봇 정보
     */
    public BotMatchStartResponse startShowdown(Long humanUserId, BotMatchStartRequest request) {
        botMatchValidator.validateHumanUserId(humanUserId);
        BotDifficulty difficulty = request.difficulty();
        botMatchValidator.validateDifficulty(difficulty);

        boolean fromMatchmaking = request.isFromMatchmaking();
        if (fromMatchmaking) {
            // 매칭 대기 → 봇 전환: 큐에 있으면 취소, 없으면 no-op
            matchService.cancelQueueIfPresent(humanUserId);
        }

        // 난이도별 덱 placeholder — 현재 드로우 경로에는 미연동
        List<Long> deckIds = botDeckCatalog.masterPitchCardIdsFor(difficulty);
        if (!deckIds.isEmpty()) {
            log.debug("bot deck catalog (unused yet) difficulty={} size={}", difficulty, deckIds.size());
        }

        Long botUserId = botUserPoolExecutor.resolveBotUserId(difficulty);
        // 공개 큐 미사용 — practiceBotMatch=true 로 직접 생성
        String matchSessionId = singleMatchCreateExecutor.execute(humanUserId, botUserId, true);

        botMatchAutoPlayService.start(matchSessionId, botUserId);
        notifyMatchFound(humanUserId, matchSessionId);

        log.info("bot match started matchSessionId={} human={} bot={} difficulty={} fromMatchmaking={}",
                matchSessionId, humanUserId, botUserId, difficulty, fromMatchmaking);

        return new BotMatchStartResponse(matchSessionId, botUserId, difficulty, fromMatchmaking);
    }

    /**
     * 사람 유저에게 매칭 성사 이벤트를 보낸다 (PvP와 동일 토픽).
     *
     * @param userId          사람 유저
     * @param matchSessionId  매치 세션
     */
    private void notifyMatchFound(Long userId, String matchSessionId) {
        messagingTemplate.convertAndSend(
                USER_MATCH_TOPIC_PREFIX + userId + "/match",
                new MatchFoundEvent(matchSessionId));
    }
}
