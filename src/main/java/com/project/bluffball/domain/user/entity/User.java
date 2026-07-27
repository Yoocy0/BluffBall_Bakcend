package com.project.bluffball.domain.user.entity;

import com.project.bluffball.domain.user.enums.UserRole;
import com.project.bluffball.global.util.DisplayWidth;
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

    /** 닉네임 — 표시 폭 ≤ 16(한글 8자), 컬럼은 영문 16자 상한 */
    @Column(name = "nickname", nullable = false, unique = true, length = 16)
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
        requireValidNickname(nickname);
        this.nickname = nickname;
    }

    /**
     * 닉네임 무료 변경 처리
     */
    public void changeNicknameForFree(String newNickname) {
        if (!this.isNicknameChangeFree) {
            throw new IllegalStateException("이미 무료 닉네임 변경 기회를 사용했습니다.");
        }
        requireValidNickname(newNickname);
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
        requireValidNickname(newNickname);
        this.currency -= cost;
        this.nickname = newNickname;
    }

    /**
     * 닉네임 표시 폭·공백 규칙을 검증한다.
     *
     * @param nickname 닉네임
     */
    private static void requireValidNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("닉네임은 비어 있을 수 없습니다.");
        }
        if (!DisplayWidth.isWithin(nickname, DisplayWidth.MAX_DISPLAY_NAME_WIDTH)) {
            throw new IllegalArgumentException(
                    "닉네임 표시 폭이 초과되었습니다. max=" + DisplayWidth.MAX_DISPLAY_NAME_WIDTH);
        }
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

    /**
     * 재화를 차감한다. (팀 기부 등)
     *
     * @param amount 차감 금액 (양수)
     * @throws IllegalArgumentException 금액이 0 이하이거나 잔액 부족 시
     */
    public void spendCurrency(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("차감 금액은 0보다 커야 합니다.");
        }
        if (this.currency < amount) {
            throw new IllegalArgumentException("보유 재화가 부족합니다.");
        }
        this.currency -= amount;
    }
}
