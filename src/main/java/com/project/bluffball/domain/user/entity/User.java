package com.project.bluffball.domain.user.entity;

import com.project.bluffball.domain.user.enums.UserRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 유저 엔터티.
 * 'user'는 MariaDB 예약어이므로 테이블명을 'users'로 지정한다.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "nickname", nullable = false, unique = true, length = 20)
    private String nickname;

    /** 유저 권한 — DB에 ordinal(0=USER, 1=ADMIN) 정수로 저장 */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "role", nullable = false)
    private UserRole role;

    /** 계정 활성 여부 (false = 정지 계정) */
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    /** 보유 재화 */
    @Column(name = "currency", nullable = false)
    private long currency;

    /** 닉네임 무료 변경 가능 여부 (가입 시 1회 부여) */
    @Column(name = "is_nickname_change_free", nullable = false)
    private boolean isNicknameChangeFree;

    /** 최초 가입 일시 — 이후 변경 불가 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
        this.currency = 0L;
        this.isNicknameChangeFree = true;
        if (this.role == null) {
            this.role = UserRole.USER;
        }
    }

    public User(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 닉네임 무료 변경 처리
     */
    public void changeNicknameForFree(String newNickname) {
        if (!this.isNicknameChangeFree) {
            throw new IllegalStateException("이미 무료 닉네임 변경 기회를 사용했습니다.");
        }
        this.nickname = newNickname;
        this.isNicknameChangeFree = false;
    }

    /**
     * 재화를 사용한 닉네임 변경 처리
     */
    public void changeNicknameWithCurrency(String newNickname, long cost) {
        if (this.currency < cost) {
            throw new IllegalArgumentException("보유 재화가 부족하여 닉네임을 변경할 수 없습니다.");
        }
        this.currency -= cost;
        this.nickname = newNickname;
    }

    /**
     * 재화 충전 또는 획득
     */
    public void addCurrency(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("추가할 재화는 음수일 수 없습니다.");
        }
        this.currency += amount;
    }
}
