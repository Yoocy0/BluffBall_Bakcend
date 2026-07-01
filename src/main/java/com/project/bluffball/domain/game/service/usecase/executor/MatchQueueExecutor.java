package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.game.enums.MatchQueueState;
import com.project.bluffball.domain.game.redis.MatchQueueEntry;
import com.project.bluffball.domain.game.repository.MatchQueueEntryRepository;
import com.project.bluffball.domain.game.repository.MatchQueueRedisOps;
import com.project.bluffball.domain.game.service.usecase.reader.MatchQueueReader;
import com.project.bluffball.domain.user.record.enums.GameMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 매칭 큐 등록·취소·매칭 성사 상태 변경 Executor.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchQueueExecutor {

    private final MatchQueueEntryRepository matchQueueEntryRepository;
    private final MatchQueueRedisOps matchQueueRedisOps;
    private final MatchQueueReader matchQueueReader;

    public Long pollOpponentOrEnqueue(Long userId, GameMode gameMode) {
        return matchQueueRedisOps.pollOpponentOrEnqueue(gameMode, userId).orElse(null);
    }

    public void registerWaiting(Long userId, GameMode gameMode) {
        matchQueueEntryRepository.save(MatchQueueEntry.waiting(userId, gameMode));
    }

    public void markMatched(Long userId, String matchSessionId) {
        MatchQueueEntry entry = matchQueueReader.getById(userId);
        entry.markMatched(matchSessionId);
        matchQueueEntryRepository.save(entry);
    }

    public void cancelWaiting(Long userId, GameMode gameMode) {
        matchQueueRedisOps.removeFromQueue(gameMode, userId);
        matchQueueEntryRepository.deleteById(userId);
    }

    /** MATCHED 등 잔여 Entry 삭제 (FIFO List 미사용) */
    public void clearEntryIfPresent(Long userId) {
        if (matchQueueEntryRepository.existsById(userId)) {
            matchQueueEntryRepository.deleteById(userId);
            log.info("매칭 큐 Entry 삭제 userId={}", userId);
        }
    }

    /**
     * 재매칭 진입 전 MATCHED 잔여 Entry를 정리한다.
     *
     * @return 정리했으면 {@code true}
     */
    public boolean evictStaleMatchedEntry(Long userId) {
        return matchQueueEntryRepository.findById(userId)
                .filter(entry -> entry.getState() == MatchQueueState.MATCHED)
                .map(entry -> {
                    matchQueueEntryRepository.deleteById(userId);
                    log.info("만료 MATCHED Entry 정리 userId={} matchSessionId={}",
                            userId, entry.getMatchSessionId());
                    return true;
                })
                .orElse(false);
    }
}
