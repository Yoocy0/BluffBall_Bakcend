package com.project.bluffball.domain.game.service.usecase.executor;



import com.project.bluffball.domain.game.redis.MatchQueueEntry;

import com.project.bluffball.domain.game.repository.MatchQueueEntryRepository;

import com.project.bluffball.domain.game.repository.MatchQueueRedisOps;

import com.project.bluffball.domain.game.service.usecase.reader.MatchQueueReader;

import com.project.bluffball.domain.user.record.enums.GameMode;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;



/**

 * 매칭 큐 등록·취소·매칭 성사 상태 변경 Executor.

 *

 * <p>Redis List(FIFO 큐)와 {@link MatchQueueEntry}의 쓰기만 담당한다.</p>

 */

@Component

@RequiredArgsConstructor

public class MatchQueueExecutor {



    /** 유저별 큐 등록 상태 Redis Hash 저장 */

    private final MatchQueueEntryRepository matchQueueEntryRepository;



    /** FIFO 매칭 큐 Redis List 연산 */

    private final MatchQueueRedisOps matchQueueRedisOps;



    /** markMatched 시 Entity 조회용 */

    private final MatchQueueReader matchQueueReader;



    /**

     * Redis FIFO 큐에서 상대를 pop하거나, 상대가 없으면 본인을 enqueue한다.

     *

     * <p>Lua 스크립트로 LPOP → 없으면 RPUSH를 원자적으로 수행한다.</p>

     *

     * @param userId   큐에 진입하는 유저 ID

     * @param gameMode 매칭 게임 모드 (큐 키 구분용)

     * @return pop된 상대 userId — 대기 등록만 한 경우 {@code null}

     */

    public Long pollOpponentOrEnqueue(Long userId, GameMode gameMode) {

        return matchQueueRedisOps.pollOpponentOrEnqueue(gameMode, userId).orElse(null);

    }



    /**

     * 큐 대기 상태로 {@link MatchQueueEntry}를 Redis에 등록한다.

     *

     * @param userId   대기 등록할 유저 ID

     * @param gameMode 매칭 게임 모드

     */

    public void registerWaiting(Long userId, GameMode gameMode) {

        matchQueueEntryRepository.save(MatchQueueEntry.waiting(userId, gameMode));

    }



    /**

     * 선매칭 유저의 큐 상태를 {@link com.project.bluffball.domain.game.enums.MatchQueueState#MATCHED}로 갱신한다.

     *

     * @param userId          선매칭(큐 선입) 유저 ID

     * @param matchSessionId  생성된 매치 세션 ID

     */

    public void markMatched(Long userId, String matchSessionId) {

        MatchQueueEntry entry = matchQueueReader.getById(userId);

        entry.markMatched(matchSessionId);

        matchQueueEntryRepository.save(entry);

    }



    /**

     * 큐 대기를 취소한다 — Redis List와 Entry를 모두 제거한다.

     *

     * @param userId   취소할 유저 ID

     * @param gameMode 매칭 게임 모드

     */

    public void cancelWaiting(Long userId, GameMode gameMode) {

        matchQueueRedisOps.removeFromQueue(gameMode, userId);

        matchQueueEntryRepository.deleteById(userId);

    }

}


