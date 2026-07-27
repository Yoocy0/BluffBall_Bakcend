package com.project.bluffball.domain.team.entity;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 리그 포맷별 팀 출전 로스터.
 *
 * <p>Compact: 타순 3명 + 전담 투수 1명(타순 밖). Full: 타순 9명(선발 투수는 타순에 포함).
 * {@code userIds} 순서 = 타순. 매칭 전 구성한다.</p>
 */
@Entity
@Table(
        name = "team_lineup",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_team_lineup_team_format",
                columnNames = {"team_id", "format"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamLineup {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_lineup_id")
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "format", nullable = false)
    private LeagueFormat format;

    /** 출전 유저 ID — 순서 = 타순 */
    @ElementCollection
    @CollectionTable(
            name = "team_lineup_member",
            joinColumns = @JoinColumn(name = "team_lineup_id")
    )
    @Column(name = "user_id", nullable = false)
    @OrderColumn(name = "slot_order")
    private List<Long> userIds = new ArrayList<>();

    /** 선발 투수 유저 ID (출전 로스터에 포함) */
    @Column(name = "starting_pitcher_user_id", nullable = false)
    private Long startingPitcherUserId;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 출전 로스터를 생성한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @param userIds 타순(출전 유저 ID)
     * @param startingPitcherUserId 선발 투수
     */
    public TeamLineup(
            Long teamId,
            LeagueFormat format,
            List<Long> userIds,
            Long startingPitcherUserId) {
        this.teamId = teamId;
        this.format = format;
        replaceLineup(userIds, startingPitcherUserId);
    }

    /**
     * 타순·선발 투수를 교체한다.
     *
     * @param userIds 타순
     * @param startingPitcherUserId 선발 투수
     */
    public void replaceLineup(List<Long> userIds, Long startingPitcherUserId) {
        this.userIds = new ArrayList<>(userIds);
        this.startingPitcherUserId = startingPitcherUserId;
        this.updatedAt = LocalDateTime.now(KST);
    }

    @PrePersist
    private void prePersist() {
        this.updatedAt = LocalDateTime.now(KST);
    }
}
