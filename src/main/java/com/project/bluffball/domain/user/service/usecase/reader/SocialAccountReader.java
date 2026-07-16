package com.project.bluffball.domain.user.service.usecase.reader;

import com.project.bluffball.domain.user.entity.SocialAccount;
import com.project.bluffball.domain.user.enums.SocialProvider;
import com.project.bluffball.domain.user.repository.SocialAccountRepository;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 소셜 계정 읽기 전담 Reader (usecase/reader 계층).
 */
@Component
@RequiredArgsConstructor
public class SocialAccountReader {

    private final SocialAccountRepository socialAccountRepository;

    SocialAccount getByProviderAndUserId(SocialProvider provider, String providerUserId) {
        return socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.SOCIAL_ACCOUNT_NOT_FOUND,
                        "provider=" + provider + ", providerUserId=" + providerUserId));
    }

    public Optional<Long> findUserIdByProviderUser(SocialProvider provider, String providerUserId) {
        return socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(SocialAccount::getUserId);
    }

    public boolean isLinked(SocialProvider provider, String providerUserId) {
        return socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(SocialAccount::isLinked)
                .orElse(false);
    }
}
