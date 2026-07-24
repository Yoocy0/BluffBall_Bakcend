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
 * <p>Compact 3명 / Full 9명. 매칭 전 구성한다.</p>
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

    /** 출전 유저 ID (순서 유지) */
    @ElementCollection
    @CollectionTable(
            name = "team_lineup_member",
            joinColumns = @JoinColumn(name = "team_lineup_id")
    )
    @Column(name = "user_id", nullable = false)
    @OrderColumn(name = "slot_order")
    private List<Long> userIds = new ArrayList<>();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 출전 로스터를 생성한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @param userIds 출전 유저 ID 목록
     */
    public TeamLineup(Long teamId, LeagueFormat format, List<Long> userIds) {
        this.teamId = teamId;
        this.format = format;
        replaceUserIds(userIds);
    }

    /**
     * 출전 유저 목록을 교체한다.
     *
     * @param userIds 새 출전 유저 ID 목록
     */
    public void replaceUserIds(List<Long> userIds) {
        this.userIds = new ArrayList<>(userIds);
        this.updatedAt = LocalDateTime.now(KST);
    }

    @PrePersist
    private void prePersist() {
        this.updatedAt = LocalDateTime.now(KST);
    }
}
