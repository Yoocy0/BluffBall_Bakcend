package com.project.bluffball.domain.league.entity;

import com.project.bluffball.domain.league.LeagueScoreConstants;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 리그 시즌 소속 팀.
 *
 * <p>참여권 구매({@link LeagueEntryTicket}) 이후 시즌에 등록된다.
 * 순위는 {@code score} 기준이며, 점수 산식은 다음과 같다.</p>
 * <ul>
 *   <li>시작: {@link LeagueScoreConstants#BASE_SCORE}</li>
 *   <li>승리: {@code score += runDiff + MATCH_POINTS}</li>
 *   <li>패배: {@code score += runDiff - MATCH_POINTS}</li>
 * </ul>
 */
@Entity
@Table(
        name = "league_team",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_league_team_season_team",
                columnNames = {"league_season_id", "team_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeagueTeam {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "league_team_id")
    private Long id;

    @Column(name = "league_season_id", nullable = false)
    private Long leagueSeasonId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    /** 구매된 참여권 ID */
    @Column(name = "league_entry_ticket_id", nullable = false)
    private Long leagueEntryTicketId;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "wins", nullable = false)
    private int wins;

    @Column(name = "loses", nullable = false)
    private int loses;

    /**
     * 누적 득실 (우리 득점 − 상대 득점).
     * 순위 산정용 {@code score}에 반영되며, 표기/타이브레이커용으로도 보관한다.
     */
    @Column(name = "run_diff", nullable = false)
    private int runDiff;

    /**
     * 순위 점수.
     * 리그 시작(등록) 시 {@link LeagueScoreConstants#BASE_SCORE}로 초기화된다.
     */
    @Column(name = "score", nullable = false)
    private int score;

    /** 순위 (0 = 미산정) */
    @Column(name = "rank", nullable = false)
    private int rank;

    @PrePersist
    private void prePersist() {
        this.joinedAt = LocalDateTime.now(KST);
    }

    public LeagueTeam(Long leagueSeasonId, Long teamId, Long leagueEntryTicketId) {
        this.leagueSeasonId = leagueSeasonId;
        this.teamId = teamId;
        this.leagueEntryTicketId = leagueEntryTicketId;
        this.wins = 0;
        this.loses = 0;
        this.runDiff = 0;
        this.score = LeagueScoreConstants.BASE_SCORE;
        this.rank = 0;
    }

    /**
     * 승리 결과를 반영한다.
     *
     * @param matchRunDiff 해당 경기 득실 (우리 득점 − 상대 득점)
     */
    public void applyWin(int matchRunDiff) {
        this.wins++;
        this.runDiff += matchRunDiff;
        this.score += matchRunDiff + LeagueScoreConstants.MATCH_POINTS;
    }

    /**
     * 패배 결과를 반영한다.
     *
     * @param matchRunDiff 해당 경기 득실 (우리 득점 − 상대 득점, 보통 음수)
     */
    public void applyLoss(int matchRunDiff) {
        this.loses++;
        this.runDiff += matchRunDiff;
        this.score += matchRunDiff - LeagueScoreConstants.MATCH_POINTS;
    }

    public void updateRank(int rank) {
        if (rank < 0) {
            throw new IllegalArgumentException("순위는 0 이상이어야 합니다.");
        }
        this.rank = rank;
    }
}
