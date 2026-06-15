package com.project.bluffball.domain.user.entity;

import com.project.bluffball.domain.user.enums.SocialProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 소셜 로그인 연동 정보 엔터티.
 * 한 유저가 카카오·구글 등 여러 소셜 계정을 연동할 수 있도록 User(1) : SocialAccount(N) 구조로 설계한다.
 *
 * providerUserId는 각 소셜 플랫폼이 발급하는 고유 식별자로, String 사용이 불가피한 유일한 예외 필드다.
 */
@Entity
@Table(
        name = "social_account",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_provider_user",
                columnNames = {"provider", "provider_user_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_account_id")
    private Long id;

    /** 이 소셜 계정의 주인 유저 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 소셜 로그인 제공자 — DB에 ordinal(0=KAKAO, 1=GOOGLE) 정수로 저장 */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "provider", nullable = false)
    private SocialProvider provider;

    /** 소셜 플랫폼이 발급한 고유 유저 식별자 (외부 의존 필드 — String 불가피) */
    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;

    /** 연동 활성 여부 (false = 연동 해제) */
    @Column(name = "is_linked", nullable = false)
    private boolean isLinked;

    public SocialAccount(Long userId, SocialProvider provider, String providerUserId) {
        this.userId = userId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.isLinked = true;
    }

    /** 소셜 연동을 해제한다. */
    public void unlink() {
        this.isLinked = false;
    }

    /** 소셜 연동을 재활성화한다. */
    public void relink() {
        this.isLinked = true;
    }
}
