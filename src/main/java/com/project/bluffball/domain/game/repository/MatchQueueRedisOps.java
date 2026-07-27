package com.project.bluffball.domain.game.repository;

import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.domain.user.record.enums.GameMode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * FIFO 매칭 큐 Redis List 연산.
 *
 * <p>쇼다운: {@code match:queue:{GameMode}}
 * 리그: {@code match:queue:{GameMode}:{LeagueTier}}</p>
 */
@Component
@RequiredArgsConstructor
public class MatchQueueRedisOps {

    /** Redis List 키 prefix */
    private static final String QUEUE_KEY_PREFIX = "match:queue:";

    /**
     * LPOP(상대 pop) → 없으면 RPUSH(본인 enqueue)를 원자적으로 수행하는 Lua 스크립트.
     */
    private static final DefaultRedisScript<String> POP_OR_ENQUEUE_SCRIPT = new DefaultRedisScript<>(
            """
                    local opponent = redis.call('LPOP', KEYS[1])
                    if opponent then
                      return opponent
                    end
                    redis.call('RPUSH', KEYS[1], ARGV[1])
                    return ''
                    """,
            String.class);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 쇼다운 큐에서 상대를 pop하거나 본인을 enqueue한다.
     *
     * @param gameMode 게임 모드
     * @param userId 진입 유저 ID
     * @return 상대 userId — 없으면 empty
     */
    public Optional<Long> pollOpponentOrEnqueue(GameMode gameMode, Long userId) {
        return pollOpponentOrEnqueue(gameMode, null, userId);
    }

    /**
     * 모드·티어 큐에서 상대를 pop하거나 본인을 enqueue한다.
     *
     * @param gameMode 게임 모드
     * @param leagueTier 리그 티어 (쇼다운은 null)
     * @param userId 진입 유저 ID
     * @return 상대 userId — 없으면 empty
     */
    public Optional<Long> pollOpponentOrEnqueue(GameMode gameMode, LeagueTier leagueTier, Long userId) {
        String result = stringRedisTemplate.execute(
                POP_OR_ENQUEUE_SCRIPT,
                List.of(queueKey(gameMode, leagueTier)),
                String.valueOf(userId));

        if (result == null || result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(result));
    }

    /**
     * 쇼다운 큐에서 유저를 제거한다.
     *
     * @param gameMode 게임 모드
     * @param userId 유저 ID
     */
    public void removeFromQueue(GameMode gameMode, Long userId) {
        removeFromQueue(gameMode, null, userId);
    }

    /**
     * 모드·티어 큐에서 유저를 제거한다.
     *
     * @param gameMode 게임 모드
     * @param leagueTier 리그 티어 (쇼다운은 null)
     * @param userId 유저 ID
     */
    public void removeFromQueue(GameMode gameMode, LeagueTier leagueTier, Long userId) {
        stringRedisTemplate.opsForList().remove(
                queueKey(gameMode, leagueTier), 1, String.valueOf(userId));
    }

    /**
     * Redis List 키를 생성한다.
     *
     * @param gameMode 게임 모드
     * @param leagueTier 리그 티어 (null이면 모드만)
     * @return 큐 키
     */
    private String queueKey(GameMode gameMode, LeagueTier leagueTier) {
        if (leagueTier == null) {
            return QUEUE_KEY_PREFIX + gameMode.name();
        }
        return QUEUE_KEY_PREFIX + gameMode.name() + ":" + leagueTier.name();
    }
}
