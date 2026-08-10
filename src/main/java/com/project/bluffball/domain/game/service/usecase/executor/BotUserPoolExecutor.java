package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.game.enums.BotDifficulty;
import com.project.bluffball.domain.user.entity.User;
import com.project.bluffball.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 난이도별 재사용 봇 유저 풀 Executor.
 *
 * <p>매치마다 새 유저를 만들지 않고, 난이도당 닉네임이 고정된 봇 1명을
 * find-or-create 한다.</p>
 *
 * <p>TODO: User에 bot 플래그/전용 풀 테이블을 도입하면 nickname 규약 대신
 * 명시적 봇 마킹으로 교체한다.</p>
 */
@Component
@RequiredArgsConstructor
public class BotUserPoolExecutor {

    /** 난이도별 고정 봇 닉네임 — DisplayWidth ≤ 16 */
    private static final String NICK_EASY = "BotEasy";
    private static final String NICK_NORMAL = "BotNormal";
    private static final String NICK_HARD = "BotHard";

    /** 유저 영속 Repository */
    private final UserRepository userRepository;

    /**
     * 난이도에 해당하는 봇 유저 ID를 반환한다. 없으면 생성한다.
     *
     * @param difficulty 봇 난이도
     * @return 봇 userId
     */
    @Transactional
    public Long resolveBotUserId(BotDifficulty difficulty) {
        String nickname = nicknameFor(difficulty);
        return userRepository.findByNickname(nickname)
                .map(User::getId)
                .orElseGet(() -> userRepository.save(new User(nickname)).getId());
    }

    /**
     * 난이도 → 고정 봇 닉네임.
     *
     * @param difficulty 난이도
     * @return 닉네임
     */
    static String nicknameFor(BotDifficulty difficulty) {
        return switch (difficulty) {
            case EASY -> NICK_EASY;
            case NORMAL -> NICK_NORMAL;
            case HARD -> NICK_HARD;
        };
    }
}
