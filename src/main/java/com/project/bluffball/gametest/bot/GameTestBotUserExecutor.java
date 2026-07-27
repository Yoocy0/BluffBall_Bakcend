package com.project.bluffball.gametest.bot;

import com.project.bluffball.domain.user.entity.User;
import com.project.bluffball.domain.user.repository.UserRepository;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 테스트용 봇 유저 생성·재화 지급 Executor.
 */
@Component
@RequiredArgsConstructor
public class GameTestBotUserExecutor {

    private final UserRepository userRepository;

    /**
     * 봇 유저를 생성한다.
     *
     * @param role 역할(닉네임 prefix)
     * @return 생성된 userId
     */
    @Transactional
    public Long createBotUser(BotRole role) {
        String prefix = role == BotRole.PITCHER ? "bP" : "bB";
        String nickname = prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        User user = userRepository.save(new User(nickname));
        return user.getId();
    }

    /**
     * 유저 재화를 추가한다.
     *
     * @param userId 유저 ID
     * @param amount 금액
     */
    @Transactional
    public void addCurrency(Long userId, long amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND, "userId=" + userId));
        user.addCurrency(amount);
        userRepository.save(user);
    }
}
