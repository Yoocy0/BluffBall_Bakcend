package com.project.bluffball.domain.user.record.entity;

import com.project.bluffball.domain.user.record.enums.GameMode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 타자 상세 성적 엔터티 (record_type_code = 2).
 *
 * <p>표시용 OPS = 출루율 + 장타율. 저장 필드로 계산한다.
 * <ul>
 *   <li>출루율 = (안타 + 볼넷) / 타석</li>
 *   <li>장타율 = (1루타 + 2*2루타 + 3*3루타 + 4*홈런) / 타수</li>
 *   <li>1루타 = 안타 - 2루타 - 3루타 - 홈런</li>
 * </ul>
 * </p>
 */
@Entity
@DiscriminatorValue("2")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BatterRecord extends UserRecord {

    /** 타석 */
    @Column(name = "plate_appearances", nullable = false)
    private int plateAppearances;

    /** 타수 */
    @Column(name = "at_bats", nullable = false)
    private int atBats;

    /** 안타 (1루타+2루타+3루타+홈런 합) */
    @Column(name = "hits", nullable = false)
    private int hits;

    /** 볼넷 */
    @Column(name = "base_on_balls", nullable = false)
    private int baseOnBalls;

    /** 2루타 */
    @Column(name = "doubles", nullable = false)
    private int doubles;

    /** 3루타 */
    @Column(name = "triples", nullable = false)
    private int triples;

    /** 홈런 */
    @Column(name = "home_runs", nullable = false)
    private int homeRuns;

    /** 타점 */
    @Column(name = "runs_batted_in", nullable = false)
    private int runsBattedIn;

    /** 삼진 */
    @Column(name = "strike_outs", nullable = false)
    private int strikeOuts;

    /** 병살타 */
    @Column(name = "double_plays", nullable = false)
    private int doublePlays;

    public BatterRecord(Long userId, GameMode gameMode, Long leagueId,
                        int totalGames, int wins, int loses,
                        int plateAppearances, int atBats, int hits, int baseOnBalls,
                        int doubles, int triples, int homeRuns, int runsBattedIn,
                        int strikeOuts, int doublePlays) {
        super(userId, gameMode, leagueId, totalGames, wins, loses);
        this.plateAppearances = plateAppearances;
        this.atBats = atBats;
        this.hits = hits;
        this.baseOnBalls = baseOnBalls;
        this.doubles = doubles;
        this.triples = triples;
        this.homeRuns = homeRuns;
        this.runsBattedIn = runsBattedIn;
        this.strikeOuts = strikeOuts;
        this.doublePlays = doublePlays;
    }
}
