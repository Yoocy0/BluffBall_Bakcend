package com.project.bluffball.domain.game.service;

import com.project.bluffball.domain.game.dto.response.MatchFoundEvent;
import com.project.bluffball.domain.game.dto.response.MatchJoinResponse;
import com.project.bluffball.domain.game.enums.MatchJoinDecision;
import com.project.bluffball.domain.game.enums.MatchQueueState;
import com.project.bluffball.domain.game.service.usecase.executor.MatchQueueExecutor;
import com.project.bluffball.domain.game.service.usecase.executor.SingleMatchCreateExecutor;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.game.service.usecase.reader.MatchQueueReader;
import com.project.bluffball.domain.game.service.usecase.validator.MatchQueueValidator;
import com.project.bluffball.domain.user.record.enums.GameMode;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.MatchNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 싱글 모드 매칭 큐 서비스.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchService {

    private static final GameMode SINGLE_MODE = GameMode.GENERAL;
    private static final String USER_MATCH_TOPIC_PREFIX = "/topic/user/";

    private final MatchQueueValidator matchQueueValidator;
    private final MatchQueueReader matchQueueReader;
    private final MatchInfoReader matchInfoReader;
    private final MatchQueueExecutor matchQueueExecutor;
    private final SingleMatchCreateExecutor singleMatchCreateExecutor;
    private final SimpMessagingTemplate messagingTemplate;

    public MatchJoinResponse joinQueue(Long userId) {
        matchQueueExecutor.evictStaleMatchedEntry(userId);
        matchQueueValidator.validateJoinAllowed(matchQueueReader.hasQueueEntry(userId));

        Long opponentUserId = matchQueueExecutor.pollOpponentOrEnqueue(userId, SINGLE_MODE);
        MatchJoinDecision decision = matchQueueValidator.resolveJoinDecision(opponentUserId, userId);

        return switch (decision) {
            case WAIT -> enqueueAndWait(userId);
            case MATCH -> completeMatch(opponentUserId, userId);
        };
    }

    public void cancelQueue(Long userId) {
        if (!matchQueueReader.hasQueueEntry(userId)) {
            throw new MatchNotFoundException(ErrorCode.MATCH_NOT_FOUND);
        }

        MatchQueueState state = matchQueueReader.getQueueState(userId);
        if (state == MatchQueueState.WAITING) {
            matchQueueExecutor.cancelWaiting(userId, SINGLE_MODE);
        } else {
            matchQueueExecutor.clearEntryIfPresent(userId);
        }
        log.info("매칭 큐 취소 userId={} state={}", userId, state);
    }

    /**
     * 경기 종료 후 참가자 큐 Entry를 정리한다 — 재매칭 409 방지.
     */
    public void clearQueueEntriesForMatch(String matchSessionId) {
        for (Long userId : matchInfoReader.getParticipantUserIds(matchSessionId)) {
            matchQueueExecutor.clearEntryIfPresent(userId);
        }
        log.info("경기 종료 큐 Entry 정리 matchSessionId={}", matchSessionId);
    }

    private MatchJoinResponse enqueueAndWait(Long userId) {
        matchQueueExecutor.registerWaiting(userId, SINGLE_MODE);
        log.info("매칭 큐 대기 등록 userId={}", userId);
        return MatchJoinResponse.waiting();
    }

    private MatchJoinResponse completeMatch(Long pitcherUserId, Long batterUserId) {
        matchQueueValidator.validateOpponentEntryExists(matchQueueReader.hasQueueEntry(pitcherUserId));

        String matchSessionId = singleMatchCreateExecutor.execute(pitcherUserId, batterUserId);
        matchQueueExecutor.markMatched(pitcherUserId, matchSessionId);
        notifyMatchFound(pitcherUserId, matchSessionId);

        log.info("매칭 성사 matchSessionId={} pitcherUserId={} batterUserId={}",
                matchSessionId, pitcherUserId, batterUserId);
        return MatchJoinResponse.matched(matchSessionId);
    }

    private void notifyMatchFound(Long userId, String matchSessionId) {
        messagingTemplate.convertAndSend(
                USER_MATCH_TOPIC_PREFIX + userId + "/match",
                new MatchFoundEvent(matchSessionId));
    }
}
