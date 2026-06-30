package com.project.bluffball.domain.user.service.usecase.reader;

import com.project.bluffball.domain.user.entity.SocialAccount;
import com.project.bluffball.domain.user.enums.SocialProvider;
import com.project.bluffball.domain.user.repository.SocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 소셜 계정 읽기 전담 Reader (usecase/reader 계층).
 *
 * <p>Service는 이 클래스의 ID·boolean 반환 메서드만 호출한다.</p>
 */
@Component
@RequiredArgsConstructor
public class SocialAccountReader {

    private final SocialAccountRepository socialAccountRepository;

    /** Executor·Reader 내부 전용 — Service에서 호출 금지 */
    SocialAccount getByProviderAndUserId(SocialProvider provider, String providerUserId) {
        return socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "소셜 계정을 찾을 수 없습니다. provider=" + provider
                                + ", providerUserId=" + providerUserId));
    }

    /**
     * 소셜 계정에 연결된 유저 ID 조회 (Service ✅).
     *
     * @return 연동 계정이 없으면 empty
     */
    public Optional<Long> findUserIdByProviderUser(SocialProvider provider, String providerUserId) {
        return socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(SocialAccount::getUserId);
    }

    /** 소셜 연동 활성 여부 (Service ✅) */
    public boolean isLinked(SocialProvider provider, String providerUserId) {
        return socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(SocialAccount::isLinked)
                .orElse(false);
    }
}
