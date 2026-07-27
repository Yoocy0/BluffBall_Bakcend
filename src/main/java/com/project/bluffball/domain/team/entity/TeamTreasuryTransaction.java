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
 * <p>기부·티어 진입비·매치 보상 등 팀 재정 변동을 추적한다.</p>
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
     * 기부·보상은 양수, 진입비는 음수로 저장한다.
     */
    @Column(name = "amount", nullable = false)
    private long amount;

    /** 기부 유저 ID — {@link TeamTreasuryTransactionType#DONATION}일 때만 사용 */
    @Column(name = "source_user_id")
    private Long sourceUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now(KST);
    }

    /**
     * 팀원 기부.
     *
     * @param teamId 팀 ID
     * @param amount 금액
     * @param sourceUserId 기부 유저
     * @return 거래
     */
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

    /**
     * 리그 티어 진입비 지불.
     *
     * @param teamId 팀 ID
     * @param amount 금액
     * @return 거래
     */
    public static TeamTreasuryTransaction entryFeePayment(Long teamId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("참여권 비용은 0보다 커야 합니다.");
        }
        TeamTreasuryTransaction transaction = new TeamTreasuryTransaction();
        transaction.teamId = teamId;
        transaction.transactionType = TeamTreasuryTransactionType.ENTRY_FEE_PAYMENT;
        transaction.amount = -amount;
        return transaction;
    }

    /**
     * 리그 경기 보상 입금.
     *
     * @param teamId 팀 ID
     * @param amount 지급액
     * @return 거래
     */
    public static TeamTreasuryTransaction matchReward(Long teamId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("경기 보상은 0보다 커야 합니다.");
        }
        TeamTreasuryTransaction transaction = new TeamTreasuryTransaction();
        transaction.teamId = teamId;
        transaction.transactionType = TeamTreasuryTransactionType.MATCH_REWARD;
        transaction.amount = amount;
        return transaction;
    }
}
