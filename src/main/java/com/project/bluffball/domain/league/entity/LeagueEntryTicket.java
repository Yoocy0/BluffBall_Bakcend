package com.project.bluffball.domain.league.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 리그 참여권 구매 기록.
 *
 * <p>팀 재정으로 참여권을 구매한 뒤 해당 시즌에 참가할 수 있다 (BM).</p>
 */
@Entity
@Table(
        name = "league_entry_ticket",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_league_entry_ticket_team_season",
                columnNames = {"team_id", "league_season_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeagueEntryTicket {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "league_entry_ticket_id")
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "league_season_id", nullable = false)
    private Long leagueSeasonId;

    /** 실제 지불한 참여권 비용 */
    @Column(name = "fee_amount", nullable = false)
    private long feeAmount;

    /** 구매를 승인한 유저 ID (보통 팀장) */
    @Column(name = "purchased_by_user_id", nullable = false)
    private Long purchasedByUserId;

    @Column(name = "purchased_at", nullable = false, updatable = false)
    private LocalDateTime purchasedAt;

    @PrePersist
    private void prePersist() {
        this.purchasedAt = LocalDateTime.now(KST);
    }

    public LeagueEntryTicket(Long teamId, Long leagueSeasonId, long feeAmount, Long purchasedByUserId) {
        this.teamId = teamId;
        this.leagueSeasonId = leagueSeasonId;
        this.feeAmount = feeAmount;
        this.purchasedByUserId = purchasedByUserId;
    }
}
