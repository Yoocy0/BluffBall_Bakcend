package com.project.bluffball.domain.user.record.entity;

import com.project.bluffball.domain.user.record.enums.GameMode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유저 성적 기록 루트 엔터티.
 *
 * SINGLE_TABLE 전략 사용 — 모든 기록 타입이 단일 'user_record' 테이블에 저장된다.
 * record_type_code 컬럼 값:
 *   1 = PitcherRecord (투수 상세 성적)
 *   2 = BatterRecord  (타자 상세 성적)
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

    /** 어떤 게임 모드에서의 기록인지 — DB에 ordinal(0=GENERAL, 1=RANK, 2=CLAN) 정수로 저장 */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "game_mode", nullable = false)
    private GameMode gameMode;

    /** 총 경기 수 */
    @Column(name = "total_games", nullable = false)
    private int totalGames;

    /** 승리 수 */
    @Column(name = "wins", nullable = false)
    private int wins;

    /** 패배 수 */
    @Column(name = "loses", nullable = false)
    private int loses;

    protected UserRecord(Long userId, GameMode gameMode, int totalGames, int wins, int loses) {
        this.userId = userId;
        this.gameMode = gameMode;
        this.totalGames = totalGames;
        this.wins = wins;
        this.loses = loses;
    }
}
