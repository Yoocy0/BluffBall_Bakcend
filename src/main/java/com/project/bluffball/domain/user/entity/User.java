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

    @Column(name = "nickname", nullable = false, length = 20)
    private String nickname;

    /** 유저 권한 — DB에 ordinal(0=USER, 1=ADMIN) 정수로 저장 */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "role", nullable = false)
    private UserRole role;

    /** 계정 활성 여부 (false = 정지 계정) */
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    /** 최초 가입 일시 — 이후 변경 불가 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
        if (this.role == null) {
            this.role = UserRole.USER;
        }
    }

    public User(String nickname) {
        this.nickname = nickname;
    }
}
