package com.project.bluffball.domain.league.entity;

import com.project.bluffball.domain.league.enums.LeagueSeasonStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 실제 진행되는 리그 시즌 인스턴스.
 *
 * <p>동일 등급({@link League})이라도 팀이 30개를 초과하면 {@code groupNumber}로 분할된다.
 * 기간 경계는 {@code Asia/Seoul}(KST) 기준으로 산정한다.
 * <ul>
 *   <li>풀 리그 — 매월 1일 시작, 28일간 진행</li>
 *   <li>컴팩트 리그 — 매주 월요일 시작, 7일간 진행</li>
 * </ul>
 * </p>
 */
@Entity
@Table(
        name = "league_season",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_league_season_period_group",
                columnNames = {"league_id", "period_label", "group_number"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeagueSeason {

    public static final int DEFAULT_MAX_TEAMS = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "league_season_id")
    private Long id;

    /** 소속 리그 등급 ({@code league.league_id}) */
    @Column(name = "league_id", nullable = false)
    private Long leagueId;

    /**
     * 기간 식별 라벨.
     * 풀 리그: {@code yyyy-MM}, 컴팩트 리그: {@code yyyy-'W'ww}
     */
    @Column(name = "period_label", nullable = false, length = 10)
    private String periodLabel;

    /** 동일 기간·등급 내 분할 그룹 번호 (1부터 시작) */
    @Column(name = "group_number", nullable = false)
    private int groupNumber;

    /** 시즌 시작 시각 (KST 기준) */
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    /** 시즌 종료 시각 (KST 기준) */
    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    /** 최대 소속 팀 수 */
    @Column(name = "max_teams", nullable = false)
    private int maxTeams;

    /** 진행 상태 */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private LeagueSeasonStatus status;

    public LeagueSeason(Long leagueId, String periodLabel, int groupNumber,
                        LocalDateTime startAt, LocalDateTime endAt) {
        this.leagueId = leagueId;
        this.periodLabel = periodLabel;
        this.groupNumber = groupNumber;
        this.startAt = startAt;
        this.endAt = endAt;
        this.maxTeams = DEFAULT_MAX_TEAMS;
        this.status = LeagueSeasonStatus.RECRUITING;
    }
}
