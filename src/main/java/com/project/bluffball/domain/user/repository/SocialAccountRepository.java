package com.project.bluffball.domain.user.repository;

import com.project.bluffball.domain.user.entity.SocialAccount;
import com.project.bluffball.domain.user.enums.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    /** provider + 소셜 플랫폼 유저 ID로 연동 계정 조회 */
    Optional<SocialAccount> findByProviderAndProviderUserId(
            SocialProvider provider, String providerUserId);
}
