package com.project.bluffball.domain.game.repository;

import com.project.bluffball.domain.user.record.enums.GameMode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 싱글 모드 FIFO 매칭 큐 Redis List 연산.
 *
 * <p>게임 모드별로 별도 List 키({@code match:queue:{GameMode}})를 사용한다.
 * {@link com.project.bluffball.domain.game.service.usecase.executor.MatchQueueExecutor}에서만 호출한다.</p>
 */
@Component
@RequiredArgsConstructor
public class MatchQueueRedisOps {

    /** Redis List 키 prefix — 뒤에 {@link GameMode#name()}을 붙인다 */
    private static final String QUEUE_KEY_PREFIX = "match:queue:";

    /**
     * LPOP(상대 pop) → 없으면 RPUSH(본인 enqueue)를 원자적으로 수행하는 Lua 스크립트.
     * 동시 join race condition 방지용.
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

    /** Redis String·List 연산 템플릿 */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 큐에서 상대를 pop하거나, 상대가 없으면 본인을 enqueue한다.
     *
     * @param gameMode 큐를 구분하는 게임 모드
     * @param userId   큐에 진입하는 유저 ID
     * @return pop된 상대 userId — enqueue만 한 경우 empty
     */
    public Optional<Long> pollOpponentOrEnqueue(GameMode gameMode, Long userId) {
        String result = stringRedisTemplate.execute(
                POP_OR_ENQUEUE_SCRIPT,
                List.of(queueKey(gameMode)),
                String.valueOf(userId));

        if (result == null || result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(result));
    }

    /**
     * 큐에서 특정 userId를 제거한다.
     *
     * @param gameMode 큐를 구분하는 게임 모드
     * @param userId   제거할 유저 ID (취소 시)
     */
    public void removeFromQueue(GameMode gameMode, Long userId) {
        stringRedisTemplate.opsForList().remove(queueKey(gameMode), 1, String.valueOf(userId));
    }

    /**
     * 게임 모드별 Redis List 키를 생성한다.
     *
     * @param gameMode 큐를 구분하는 게임 모드
     * @return 예: {@code match:queue:GENERAL}
     */
    private String queueKey(GameMode gameMode) {
        return QUEUE_KEY_PREFIX + gameMode.name();
    }
}
