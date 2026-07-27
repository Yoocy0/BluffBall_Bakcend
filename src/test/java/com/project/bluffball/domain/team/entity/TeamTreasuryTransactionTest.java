package com.project.bluffball.domain.team.entity;

import com.project.bluffball.domain.team.enums.TeamTreasuryTransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TeamTreasuryTransaction")
class TeamTreasuryTransactionTest {

    @Test
    @DisplayName("기부 거래를 생성한다")
    void donation() {
        TeamTreasuryTransaction tx = TeamTreasuryTransaction.donation(10L, 200L, 5L);

        assertThat(tx.getTeamId()).isEqualTo(10L);
        assertThat(tx.getTransactionType()).isEqualTo(TeamTreasuryTransactionType.DONATION);
        assertThat(tx.getAmount()).isEqualTo(200L);
        assertThat(tx.getSourceUserId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("진입비 거래는 음수로 저장된다")
    void entryFeePayment() {
        TeamTreasuryTransaction tx = TeamTreasuryTransaction.entryFeePayment(10L, 1000L);

        assertThat(tx.getTransactionType()).isEqualTo(TeamTreasuryTransactionType.ENTRY_FEE_PAYMENT);
        assertThat(tx.getAmount()).isEqualTo(-1000L);
        assertThat(tx.getSourceUserId()).isNull();
    }

    @Test
    @DisplayName("매치 보상 거래를 생성한다")
    void matchReward() {
        TeamTreasuryTransaction tx = TeamTreasuryTransaction.matchReward(10L, 100L);

        assertThat(tx.getTransactionType()).isEqualTo(TeamTreasuryTransactionType.MATCH_REWARD);
        assertThat(tx.getAmount()).isEqualTo(100L);
    }

    @Test
    @DisplayName("금액이 0 이하면 팩토리가 예외를 던진다")
    void nonPositiveAmount_throws() {
        assertThatThrownBy(() -> TeamTreasuryTransaction.donation(1L, 0L, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TeamTreasuryTransaction.entryFeePayment(1L, -1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TeamTreasuryTransaction.matchReward(1L, 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
