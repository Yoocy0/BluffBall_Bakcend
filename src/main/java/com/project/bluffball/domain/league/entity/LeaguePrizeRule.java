package com.project.bluffball.domain.league.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 리그 순위별 상금 비율 규칙.
 *
 * <p>전 리그 공통으로 사용한다. 실제 상금액은
 * {@link League#getFirstPlacePrize()} × {@code percentOfFirst / 100} 으로 산정한다.</p>
 *
 * <p>예: 1등 100%, 2등 50%, 3등 30% …</p>
 */
@Entity
@Table(
        name = "league_prize_rule",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_league_prize_rule_rank",
                columnNames = {"rank_position"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeaguePrizeRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "league_prize_rule_id")
    private Long id;

    /** 순위 (1부터 시작) */
    @Column(name = "rank_position", nullable = false)
    private int rankPosition;

    /** 1등 상금 대비 퍼센트 (1등=100) */
    @Column(name = "percent_of_first", nullable = false)
    private int percentOfFirst;

    public LeaguePrizeRule(int rankPosition, int percentOfFirst) {
        if (rankPosition < 1) {
            throw new IllegalArgumentException("순위는 1 이상이어야 합니다.");
        }
        if (percentOfFirst < 0 || percentOfFirst > 100) {
            throw new IllegalArgumentException("상금 비율은 0~100 이어야 합니다.");
        }
        this.rankPosition = rankPosition;
        this.percentOfFirst = percentOfFirst;
    }
}
