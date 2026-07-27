package com.project.bluffball.domain.team.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Team 엔티티")
class TeamTest {

    @Test
    @DisplayName("생성 시 금고는 0이다")
    void create_initialTreasuryIsZero() {
        Team team = new Team("블러프", 1L);

        assertThat(team.getName()).isEqualTo("블러프");
        assertThat(team.getLeaderUserId()).isEqualTo(1L);
        assertThat(team.getTreasury()).isZero();
        assertThat(team.getLogoUrl()).isNull();
    }

    @Test
    @DisplayName("로고를 변경한다")
    void updateLogo() {
        Team team = new Team("블러프", 1L, "old.png");

        team.updateLogo("new.png");

        assertThat(team.getLogoUrl()).isEqualTo("new.png");
    }

    @Test
    @DisplayName("팀장을 변경한다")
    void changeLeader() {
        Team team = new Team("블러프", 1L);

        team.changeLeader(2L);

        assertThat(team.getLeaderUserId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("팀장 ID가 null이면 예외")
    void changeLeader_null_throws() {
        Team team = new Team("블러프", 1L);

        assertThatThrownBy(() -> team.changeLeader(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Nested
    @DisplayName("금고")
    class Treasury {

        @Test
        @DisplayName("기부를 받으면 잔액이 증가한다")
        void receiveDonation() {
            Team team = new Team("블러프", 1L);

            team.receiveDonation(500L);

            assertThat(team.getTreasury()).isEqualTo(500L);
        }

        @Test
        @DisplayName("기부 금액이 0 이하면 예외")
        void receiveDonation_nonPositive_throws() {
            Team team = new Team("블러프", 1L);

            assertThatThrownBy(() -> team.receiveDonation(0L))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> team.receiveDonation(-1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("경기 보상을 받으면 잔액이 증가한다")
        void addMatchReward() {
            Team team = new Team("블러프", 1L);

            team.addMatchReward(100L);

            assertThat(team.getTreasury()).isEqualTo(100L);
        }

        @Test
        @DisplayName("참가비를 지불하면 잔액이 감소한다")
        void payEntryFee() {
            Team team = new Team("블러프", 1L);
            team.receiveDonation(1000L);

            team.payEntryFee(300L);

            assertThat(team.getTreasury()).isEqualTo(700L);
        }

        @Test
        @DisplayName("참가비보다 잔액이 부족하면 예외")
        void payEntryFee_insufficient_throws() {
            Team team = new Team("블러프", 1L);
            team.receiveDonation(100L);

            assertThatThrownBy(() -> team.payEntryFee(101L))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(team.getTreasury()).isEqualTo(100L);
        }

        @Test
        @DisplayName("참가비가 0 이하면 예외")
        void payEntryFee_nonPositive_throws() {
            Team team = new Team("블러프", 1L);
            team.receiveDonation(100L);

            assertThatThrownBy(() -> team.payEntryFee(0L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
