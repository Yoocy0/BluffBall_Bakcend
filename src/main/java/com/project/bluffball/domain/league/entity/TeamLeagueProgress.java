package com.project.bluffball.domain.league.entity;

import com.project.bluffball.domain.league.LeagueScoreConstants;
import com.project.bluffball.domain.league.config.LeagueTierRule;
import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 팀의 포맷별 리그 진행 상태 (상시 티어·점수).
 *
 * <p>시즌 개폐 없이 Compact/Full 각각 현재 티어와 점수를 유지한다.
 * 점수는 티어 밴드로 클램프되며, 상한 도달 시 상위 티어 참가 자격이 생긴다.</p>
 */
@Entity
@Table(
        name = "team_league_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_team_league_progress_team_format",
                columnNames = {"team_id", "format"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamLeagueProgress {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_league_progress_id")
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "format", nullable = false)
    private LeagueFormat format;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "current_tier", nullable = false)
    private LeagueTier currentTier;

    /** 티어 밴드 내 점수 (0부터, 상한에서 클램프) */
    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "wins", nullable = false)
    private int wins;

    @Column(name = "loses", nullable = false)
    private int loses;

    @Column(name = "run_diff", nullable = false)
    private int runDiff;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 최하위 티어로 신규 진행한다.
     *
     * @param teamId 팀 ID
     * @param format 포맷
     */
    public TeamLeagueProgress(Long teamId, LeagueFormat format) {
        this.teamId = teamId;
        this.format = format;
        this.currentTier = LeagueTierRule.lowestTier();
        this.rating = 0;
        this.wins = 0;
        this.loses = 0;
        this.runDiff = 0;
        touch();
    }

    /**
     * 경기 결과를 점수·전적에 반영한다.
     *
     * <p>이미 티어 상한({@code ceil})에 있으면 승리해도 rating은 오르지 않는다(+0).
     * 전적·득실은 반영한다. 패배는 상한에서도 점수가 내려갈 수 있다.</p>
     *
     * @param won 승리 여부
     * @param matchRunDiff 우리 득점 − 상대 득점
     */
    public void applyMatchResult(boolean won, int matchRunDiff) {
        LeagueTierRule rule = LeagueTierRule.of(currentTier);
        if (won) {
            this.wins++;
            this.runDiff += matchRunDiff;
            if (this.rating < rule.ceil()) {
                this.rating += LeagueScoreConstants.MATCH_POINTS + matchRunDiff;
                this.rating = rule.clamp(this.rating);
            }
        } else {
            this.loses++;
            this.runDiff += matchRunDiff;
            this.rating += matchRunDiff - LeagueScoreConstants.MATCH_POINTS;
            this.rating = rule.clamp(this.rating);
        }
        touch();
    }

    /**
     * 주기 배치용 점수 감쇠를 적용한다.
     *
     * <p>{@link LeagueScoreConstants#PERIODIC_RATING_DECAY}만큼 차감한 뒤 상한·0으로 클램프한다.
     * 감쇠 후 {@link #shouldDemote()}가 true면 강등 대상이다.</p>
     */
    public void applyPeriodicDecay() {
        this.rating -= LeagueScoreConstants.PERIODIC_RATING_DECAY;
        this.rating = LeagueTierRule.of(currentTier).clamp(this.rating);
        touch();
    }

    /**
     * 상위 티어로 승급한다. 점수는 새 티어 floor로 맞춘다.
     *
     * @param nextTier 상위 티어
     */
    public void promoteTo(LeagueTier nextTier) {
        this.currentTier = nextTier;
        LeagueTierRule rule = LeagueTierRule.of(nextTier);
        this.rating = rule.floor();
        touch();
    }

    /**
     * 하위 티어로 강등한다. 점수는 새 티어 ceil로 맞춘다.
     *
     * @param previousTier 하위 티어
     */
    public void demoteTo(LeagueTier previousTier) {
        this.currentTier = previousTier;
        LeagueTierRule rule = LeagueTierRule.of(previousTier);
        this.rating = rule.ceil();
        touch();
    }

    /**
     * 상위 진출 자격이 있는지.
     *
     * @return 자격 있으면 true
     */
    public boolean isPromoteReady() {
        return LeagueTierRule.of(currentTier).isPromoteReady(rating);
    }

    /**
     * 강등 대상인지.
     *
     * @return 강등이면 true
     */
    public boolean shouldDemote() {
        return LeagueTierRule.of(currentTier).shouldDemote(rating);
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now(KST);
    }

    @PrePersist
    private void prePersist() {
        touch();
    }
}
