package com.project.bluffball.domain.user.record.entity;

import com.project.bluffball.domain.user.record.enums.GameMode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유저 성적 기록 루트 엔터티.
 *
 * <p>SINGLE_TABLE 전략 — 모든 기록 타입이 단일 {@code user_record} 테이블에 저장된다.
 * {@code record_type_code} 컬럼 값:
 *   1 = PitcherRecord (투수 상세 성적)
 *   2 = BatterRecord  (타자 상세 성적)</p>
 *
 * <p>기록 키:
 * <ul>
 *   <li>쇼다운/커스텀 — {@code gameMode} + {@code leagueId = null}</li>
 *   <li>풀/컴팩트 리그 — {@code gameMode} + {@code leagueId} (리그 티어 단위)</li>
 * </ul>
 * </p>
 */
@Entity
@Table(name = "user_record")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "record_type_code", discriminatorType = DiscriminatorType.INTEGER)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long id;

    /** 이 기록의 주인 유저 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 게임 모드 — DB에 ordinal(0=SHOWDOWN, 1=COMPACT_LEAGUE, 2=FULL_LEAGUE, 3=CUSTOM, 4=BOT) 정수로 저장.
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "game_mode", nullable = false)
    private GameMode gameMode;

    /**
     * 리그 ID 참조 ({@code league.league_id}).
     * 쇼다운은 null, 풀·컴팩트 리그는 해당 티어 리그 ID.
     */
    @Column(name = "league_id")
    private Long leagueId;

    /** 총 경기 수 */
    @Column(name = "total_games", nullable = false)
    private int totalGames;

    /** 승리 수 */
    @Column(name = "wins", nullable = false)
    private int wins;

    /** 패배 수 */
    @Column(name = "loses", nullable = false)
    private int loses;

    protected UserRecord(Long userId, GameMode gameMode, Long leagueId,
                         int totalGames, int wins, int loses) {
        this.userId = userId;
        this.gameMode = gameMode;
        this.leagueId = leagueId;
        this.totalGames = totalGames;
        this.wins = wins;
        this.loses = loses;
    }
}
