package com.project.bluffball.domain.user.service.usecase.executor;

import com.project.bluffball.domain.user.entity.SocialAccount;
import com.project.bluffball.domain.user.entity.User;
import com.project.bluffball.domain.user.enums.SocialProvider;
import com.project.bluffball.domain.user.repository.SocialAccountRepository;
import com.project.bluffball.domain.user.repository.UserRepository;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 소셜 로그인 쓰기 전담 Executor (usecase/executor 계층).
 *
 * <p>신규 유저·소셜 계정 생성 및 연동 재활성화를 담당한다.</p>
 */
@Component
@RequiredArgsConstructor
public class SocialLoginExecutor {

    private static final String NICKNAME_PREFIX = "Player_";

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    /**
     * 신규 유저와 소셜 계정을 생성한다.
     *
     * @return 생성된 유저 ID
     */
    @Transactional
    public Long registerNewUser(SocialProvider provider, String providerUserId) {
        User user = userRepository.save(new User(generateNickname()));
        socialAccountRepository.save(new SocialAccount(user.getId(), provider, providerUserId));
        return user.getId();
    }

    /**
     * 연동 해제된 소셜 계정을 재활성화한다.
     */
    @Transactional
    public void relink(SocialProvider provider, String providerUserId) {
        SocialAccount account = socialAccountRepository
                .findByProviderAndProviderUserId(provider, providerUserId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.SOCIAL_ACCOUNT_NOT_FOUND,
                        "provider=" + provider + ", providerUserId=" + providerUserId));
        account.relink();
    }

    /** 서버 자동 생성 닉네임 (Player_ + 8자리) */
    private String generateNickname() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return NICKNAME_PREFIX + suffix;
    }
}
