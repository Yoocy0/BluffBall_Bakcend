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
 * 리그 출전 멤버의 구종·강화 사전 선택 (n+1 핸드).
 *
 * <p>{@code dropCardId}는 투수 교체 시 빠지는 +1장이다.</p>
 */
@Entity
@Table(
        name = "team_pitch_loadout",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_team_pitch_loadout_team_format_user",
                columnNames = {"team_id", "format", "user_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamPitchLoadout {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_pitch_loadout_id")
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "format", nullable = false)
    private LeagueFormat format;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 구종·강화 카드 ID (n+1장, 순서 유지) */
    @ElementCollection
    @CollectionTable(
            name = "team_pitch_loadout_card",
            joinColumns = @JoinColumn(name = "team_pitch_loadout_id")
    )
    @Column(name = "card_id", nullable = false)
    @OrderColumn(name = "card_order")
    private List<Long> cardIds = new ArrayList<>();

    /** 교체 시 제외할 카드 ID ({@code cardIds}에 포함) */
    @Column(name = "drop_card_id", nullable = false)
    private Long dropCardId;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 사전 선택을 생성한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @param userId 멤버 유저 ID
     * @param cardIds 구종·강화 카드 ID
     * @param dropCardId 교체 시 제외 카드 ID
     */
    public TeamPitchLoadout(
            Long teamId,
            LeagueFormat format,
            Long userId,
            List<Long> cardIds,
            Long dropCardId) {
        this.teamId = teamId;
        this.format = format;
        this.userId = userId;
        replaceCards(cardIds, dropCardId);
    }

    /**
     * 카드 선택과 drop 카드를 교체한다.
     *
     * @param cardIds 구종·강화 카드 ID
     * @param dropCardId 교체 시 제외 카드 ID
     */
    public void replaceCards(List<Long> cardIds, Long dropCardId) {
        this.cardIds = new ArrayList<>(cardIds);
        this.dropCardId = dropCardId;
        this.updatedAt = LocalDateTime.now(KST);
    }

    @PrePersist
    private void prePersist() {
        this.updatedAt = LocalDateTime.now(KST);
    }
}
