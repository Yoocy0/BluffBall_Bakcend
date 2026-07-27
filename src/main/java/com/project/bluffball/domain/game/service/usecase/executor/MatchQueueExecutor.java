package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.game.enums.MatchQueueState;
import com.project.bluffball.domain.game.redis.MatchQueueEntry;
import com.project.bluffball.domain.game.repository.MatchQueueEntryRepository;
import com.project.bluffball.domain.game.repository.MatchQueueRedisOps;
import com.project.bluffball.domain.game.service.usecase.reader.MatchQueueReader;
import com.project.bluffball.domain.league.enums.LeagueTier;
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

    /**
     * 쇼다운 큐에서 상대를 pop하거나 enqueue한다.
     *
     * @param userId 유저 ID
     * @param gameMode 게임 모드
     * @return 상대 userId — 없으면 null
     */
    public Long pollOpponentOrEnqueue(Long userId, GameMode gameMode) {
        return matchQueueRedisOps.pollOpponentOrEnqueue(gameMode, userId).orElse(null);
    }

    /**
     * 리그 큐(format×tier)에서 상대를 pop하거나 enqueue한다.
     *
     * @param userId 유저 ID
     * @param gameMode COMPACT_LEAGUE / FULL_LEAGUE
     * @param leagueTier 리그 단계
     * @return 상대 userId — 없으면 null
     */
    public Long pollOpponentOrEnqueue(Long userId, GameMode gameMode, LeagueTier leagueTier) {
        return matchQueueRedisOps.pollOpponentOrEnqueue(gameMode, leagueTier, userId).orElse(null);
    }

    /**
     * 쇼다운 WAITING Entry를 저장한다.
     *
     * @param userId 유저 ID
     * @param gameMode 게임 모드
     */
    public void registerWaiting(Long userId, GameMode gameMode) {
        matchQueueEntryRepository.save(MatchQueueEntry.waiting(userId, gameMode));
    }

    /**
     * 리그 WAITING Entry를 저장한다.
     *
     * @param userId 유저 ID
     * @param gameMode 게임 모드
     * @param leagueTier 리그 단계
     * @param teamId 팀 ID
     */
    public void registerWaitingLeague(
            Long userId,
            GameMode gameMode,
            LeagueTier leagueTier,
            Long teamId) {
        matchQueueEntryRepository.save(
                MatchQueueEntry.waitingLeague(userId, gameMode, leagueTier, teamId));
    }

    /**
     * 매칭 성사 상태로 전환한다.
     *
     * @param userId 유저 ID
     * @param matchSessionId 매치 세션 ID
     */
    public void markMatched(Long userId, String matchSessionId) {
        MatchQueueEntry entry = matchQueueReader.getById(userId);
        entry.markMatched(matchSessionId);
        matchQueueEntryRepository.save(entry);
    }

    /**
     * 쇼다운 대기 큐를 취소한다.
     *
     * @param userId 유저 ID
     * @param gameMode 게임 모드
     */
    public void cancelWaiting(Long userId, GameMode gameMode) {
        matchQueueRedisOps.removeFromQueue(gameMode, userId);
        matchQueueEntryRepository.deleteById(userId);
    }

    /**
     * 리그 대기 큐를 취소한다.
     *
     * @param userId 유저 ID
     * @param gameMode 게임 모드
     * @param leagueTier 리그 단계
     */
    public void cancelWaiting(Long userId, GameMode gameMode, LeagueTier leagueTier) {
        matchQueueRedisOps.removeFromQueue(gameMode, leagueTier, userId);
        matchQueueEntryRepository.deleteById(userId);
    }

    /**
     * MATCHED 등 잔여 Entry를 삭제한다.
     *
     * @param userId 유저 ID
     */
    public void clearEntryIfPresent(Long userId) {
        if (matchQueueEntryRepository.existsById(userId)) {
            matchQueueEntryRepository.deleteById(userId);
            log.info("매칭 큐 Entry 삭제 userId={}", userId);
        }
    }

    /**
     * 재매칭 진입 전 MATCHED 잔여 Entry를 정리한다.
     *
     * @param userId 유저 ID
     * @return 정리했으면 true
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
