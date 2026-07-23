package com.project.bluffball.domain.team.entity;

import com.project.bluffball.domain.team.enums.TeamTreasuryTransactionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 팀 재정 거래 내역.
 *
 * <p>기부·시즌 상금·참여권 구매 등 팀 재정 변동을 추적한다.</p>
 */
@Entity
@Table(name = "team_treasury_transaction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamTreasuryTransaction {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_treasury_transaction_id")
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "transaction_type", nullable = false)
    private TeamTreasuryTransactionType transactionType;

    /**
     * 거래 금액.
     * 기부·상금은 양수, 참여권 구매는 음수로 저장한다.
     */
    @Column(name = "amount", nullable = false)
    private long amount;

    /** 기부 유저 ID — {@link TeamTreasuryTransactionType#DONATION}일 때만 사용 */
    @Column(name = "source_user_id")
    private Long sourceUserId;

    /** 관련 시즌 ID — 상금·참여권 구매 시 사용 */
    @Column(name = "league_season_id")
    private Long leagueSeasonId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now(KST);
    }

    public static TeamTreasuryTransaction donation(Long teamId, long amount, Long sourceUserId) {
        if (amount <= 0) {
            throw new IllegalArgumentException("기부 금액은 0보다 커야 합니다.");
        }
        TeamTreasuryTransaction transaction = new TeamTreasuryTransaction();
        transaction.teamId = teamId;
        transaction.transactionType = TeamTreasuryTransactionType.DONATION;
        transaction.amount = amount;
        transaction.sourceUserId = sourceUserId;
        return transaction;
    }

    public static TeamTreasuryTransaction seasonPrize(Long teamId, long amount, Long leagueSeasonId) {
        if (amount <= 0) {
            throw new IllegalArgumentException("상금은 0보다 커야 합니다.");
        }
        TeamTreasuryTransaction transaction = new TeamTreasuryTransaction();
        transaction.teamId = teamId;
        transaction.transactionType = TeamTreasuryTransactionType.SEASON_PRIZE;
        transaction.amount = amount;
        transaction.leagueSeasonId = leagueSeasonId;
        return transaction;
    }

    public static TeamTreasuryTransaction entryFeePayment(Long teamId, long amount, Long leagueSeasonId) {
        if (amount <= 0) {
            throw new IllegalArgumentException("참여권 비용은 0보다 커야 합니다.");
        }
        TeamTreasuryTransaction transaction = new TeamTreasuryTransaction();
        transaction.teamId = teamId;
        transaction.transactionType = TeamTreasuryTransactionType.ENTRY_FEE_PAYMENT;
        transaction.amount = -amount;
        transaction.leagueSeasonId = leagueSeasonId;
        return transaction;
    }
}
