package com.project.bluffball.domain.league.entity;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 리그 등급 카탈로그 엔터티.
 *
 * <p>풀/컴팩트 × 7티어 = 최대 14개 행이 존재한다.
 * 유저 성적({@code user_record.league_id})은 이 등급을 참조한다.
 * 실제 진행 단위는 {@link LeagueSeason}이다.</p>
 */
@Entity
@Table(
        name = "league",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_league_format_tier",
                columnNames = {"format", "tier"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class League {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "league_id")
    private Long id;

    /** 풀 리그 / 컴팩트 리그 */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "format", nullable = false)
    private LeagueFormat format;

    /** 아마 1~4부 · 독립 · 프로 1~2부 */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "tier", nullable = false)
    private LeagueTier tier;

    /** 표시용 이름 (예: 풀 리그 아마 1부) */
    @Column(name = "name", nullable = false, length = 40)
    private String name;

    /** 리그 참여권 가격 (팀 재정으로 지불) */
    @Column(name = "entry_fee", nullable = false)
    private long entryFee;

    /**
     * 시즌 1등 상금.
     * 2등 이후 상금은 {@link LeaguePrizeRule}의 1등 대비 퍼센트로 산정한다.
     */
    @Column(name = "first_place_prize", nullable = false)
    private long firstPlacePrize;

    /** 리그 참가에 필요한 최소 팀원 수 */
    @Column(name = "min_team_members", nullable = false)
    private int minTeamMembers;

    public League(LeagueFormat format, LeagueTier tier, String name, long entryFee,
                  long firstPlacePrize, int minTeamMembers) {
        if (firstPlacePrize < 0) {
            throw new IllegalArgumentException("1등 상금은 0 이상이어야 합니다.");
        }
        if (minTeamMembers < 1) {
            throw new IllegalArgumentException("최소 팀원 수는 1 이상이어야 합니다.");
        }
        this.format = format;
        this.tier = tier;
        this.name = name;
        this.entryFee = entryFee;
        this.firstPlacePrize = firstPlacePrize;
        this.minTeamMembers = minTeamMembers;
    }

    /**
     * 순위별 상금액을 계산한다.
     *
     * @param percentOfFirst 1등 상금 대비 퍼센트 (1등=100)
     */
    public long calculatePrizeAmount(int percentOfFirst) {
        if (percentOfFirst < 0) {
            throw new IllegalArgumentException("상금 비율은 0 이상이어야 합니다.");
        }
        return this.firstPlacePrize * percentOfFirst / 100L;
    }
}
