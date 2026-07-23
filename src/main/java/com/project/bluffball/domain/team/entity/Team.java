package com.project.bluffball.domain.team.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 팀(클랜) 엔터티.
 *
 * <p>팀 재정({@code treasury})은 팀원 기부 또는 시즌 상금으로 충당하며,
 * 리그 참여권 구매에 사용된다.</p>
 */
@Entity
@Table(name = "team")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 30)
    private String name;

    /** 팀장 유저 ID */
    @Column(name = "leader_user_id", nullable = false)
    private Long leaderUserId;

    /** 팀 재정 (기부·상금 적립, 참여권 구매에 사용) */
    @Column(name = "treasury", nullable = false)
    private long treasury;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now(KST);
    }

    public Team(String name, Long leaderUserId) {
        this.name = name;
        this.leaderUserId = leaderUserId;
        this.treasury = 0L;
    }

    /**
     * 팀원 기부로 재정을 충당한다.
     */
    public void receiveDonation(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("기부 금액은 0보다 커야 합니다.");
        }
        this.treasury += amount;
    }

    /**
     * 시즌 성적 상금으로 재정을 충당한다.
     */
    public void addSeasonPrize(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("상금은 0보다 커야 합니다.");
        }
        this.treasury += amount;
    }

    /**
     * 리그 참여권 구매 비용을 지불한다.
     */
    public void payEntryFee(long cost) {
        if (cost <= 0) {
            throw new IllegalArgumentException("참여권 비용은 0보다 커야 합니다.");
        }
        if (this.treasury < cost) {
            throw new IllegalArgumentException("팀 재정이 부족하여 리그 참여권을 구매할 수 없습니다.");
        }
        this.treasury -= cost;
    }
}
