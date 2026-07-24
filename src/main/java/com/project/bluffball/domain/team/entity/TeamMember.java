package com.project.bluffball.domain.team.entity;

import com.project.bluffball.domain.team.enums.TeamMemberRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 팀원 소속 엔터티.
 */
@Entity
@Table(
        name = "team_member",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_team_member_team_user",
                columnNames = {"team_id", "user_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamMember {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_member_id")
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "role", nullable = false)
    private TeamMemberRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    private void prePersist() {
        this.joinedAt = LocalDateTime.now(KST);
    }

    public TeamMember(Long teamId, Long userId, TeamMemberRole role) {
        this.teamId = teamId;
        this.userId = userId;
        this.role = role;
    }

    /**
     * 팀 내 계급을 변경한다.
     */
    public void changeRole(TeamMemberRole role) {
        if (role == null) {
            throw new IllegalArgumentException("계급은 null일 수 없습니다.");
        }
        this.role = role;
    }
}
