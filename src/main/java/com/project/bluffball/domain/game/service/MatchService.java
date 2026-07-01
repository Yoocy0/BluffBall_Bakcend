package com.project.bluffball.domain.game.service;



import com.project.bluffball.domain.game.dto.response.MatchFoundEvent;

import com.project.bluffball.domain.game.dto.response.MatchJoinResponse;

import com.project.bluffball.domain.game.enums.MatchJoinDecision;

import com.project.bluffball.domain.game.service.usecase.executor.MatchQueueExecutor;

import com.project.bluffball.domain.game.service.usecase.executor.SingleMatchCreateExecutor;

import com.project.bluffball.domain.game.service.usecase.reader.MatchQueueReader;

import com.project.bluffball.domain.game.service.usecase.validator.MatchQueueValidator;

import com.project.bluffball.domain.user.record.enums.GameMode;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import org.springframework.stereotype.Service;



/**

 * 싱글 모드 매칭 큐 서비스.

 *

 * <p>큐 진입·취소를 조립하고, 매칭 성사 시 개인 WebSocket 토픽으로 알린다.

 * 조건 검증은 {@link MatchQueueValidator}, 조회는 {@link MatchQueueReader},

 * 상태 변경은 Executor에 위임한다.</p>

 */

@Service

@RequiredArgsConstructor

@Slf4j

public class MatchService {



    /** MVP 싱글 모드 — 1 vs 1 / 1이닝 ({@link GameMode#GENERAL}) */

    private static final GameMode SINGLE_MODE = GameMode.GENERAL;



    /** 대기 유저 매칭 성사 알림 WebSocket 토픽 prefix — 뒤에 {@code {userId}/match}를 붙인다 */

    private static final String USER_MATCH_TOPIC_PREFIX = "/topic/user/";



    /** 매칭 큐 진입·취소 조건 검증 */

    private final MatchQueueValidator matchQueueValidator;



    /** 매칭 큐 등록 상태 조회 */

    private final MatchQueueReader matchQueueReader;



    /** 매칭 큐 Redis List·Entry 쓰기 */

    private final MatchQueueExecutor matchQueueExecutor;



    /** MatchInfo·GameState seed 및 matchSessionId 발급 */

    private final SingleMatchCreateExecutor singleMatchCreateExecutor;



    /** 매칭 성사 WebSocket 이벤트 발행 */

    private final SimpMessagingTemplate messagingTemplate;



    /**

     * 싱글 모드 매칭 큐에 진입한다.

     *

     * <p>선매칭 유저(큐 선입)가 투수, 후매칭 유저(큐 진입)가 타자로 배정된다.</p>

     *

     * @param userId JWT로 인증된 요청 유저 ID

     * @return {@code WAITING} — 큐 대기 / {@code MATCHED} — 즉시 매칭 성사 + matchSessionId

     */

    public MatchJoinResponse joinQueue(Long userId) {

        // 재진입 방지 — 이미 큐에 등록된 유저는 409

        matchQueueValidator.validateJoinAllowed(matchQueueReader.hasQueueEntry(userId));



        // Redis FIFO 큐에서 상대 pop, 없으면 본인 enqueue

        Long opponentUserId = matchQueueExecutor.pollOpponentOrEnqueue(userId, SINGLE_MODE);

        // poll 결과에 따라 대기 vs 즉시 매칭 분기 결정

        MatchJoinDecision decision = matchQueueValidator.resolveJoinDecision(opponentUserId, userId);



        return switch (decision) {

            case WAIT -> enqueueAndWait(userId);

            case MATCH -> completeMatch(opponentUserId, userId);

        };

    }



    /**

     * 대기 중인 매칭 큐에서 이탈한다.

     *

     * @param userId JWT로 인증된 요청 유저 ID

     */

    public void cancelQueue(Long userId) {

        matchQueueValidator.validateCancelRequest(

                matchQueueReader.hasQueueEntry(userId),

                matchQueueReader.getQueueState(userId));



        matchQueueExecutor.cancelWaiting(userId, SINGLE_MODE);

        log.info("매칭 큐 취소 userId={}", userId);

    }



    /**

     * 상대가 없을 때 큐 대기 등록 후 WAITING 응답을 반환한다.

     *

     * @param userId 대기 등록할 유저 ID

     */

    private MatchJoinResponse enqueueAndWait(Long userId) {

        matchQueueExecutor.registerWaiting(userId, SINGLE_MODE);

        log.info("매칭 큐 대기 등록 userId={}", userId);

        return MatchJoinResponse.waiting();

    }



    /**

     * 두 유저 매칭을 성사시키고, 선매칭 유저에게 WebSocket 알림을 전송한다.

     *

     * @param pitcherUserId 선매칭(큐 선입) 유저 — 초기 투수

     * @param batterUserId  후매칭(큐 진입) 유저 — 초기 타자

     */

    private MatchJoinResponse completeMatch(Long pitcherUserId, Long batterUserId) {

        matchQueueValidator.validateOpponentEntryExists(matchQueueReader.hasQueueEntry(pitcherUserId));



        String matchSessionId = singleMatchCreateExecutor.execute(pitcherUserId, batterUserId);

        matchQueueExecutor.markMatched(pitcherUserId, matchSessionId);

        notifyMatchFound(pitcherUserId, matchSessionId);



        log.info("매칭 성사 matchSessionId={} pitcherUserId={} batterUserId={}",

                matchSessionId, pitcherUserId, batterUserId);

        return MatchJoinResponse.matched(matchSessionId);

    }



    /**

     * 대기 중이던 유저에게 매칭 성사 알림을 WebSocket으로 전송한다.

     *

     * @param userId          알림 수신 유저 ID (선매칭 유저)

     * @param matchSessionId  생성된 매치 세션 ID

     */

    private void notifyMatchFound(Long userId, String matchSessionId) {

        messagingTemplate.convertAndSend(

                USER_MATCH_TOPIC_PREFIX + userId + "/match",

                new MatchFoundEvent(matchSessionId));

    }

}


