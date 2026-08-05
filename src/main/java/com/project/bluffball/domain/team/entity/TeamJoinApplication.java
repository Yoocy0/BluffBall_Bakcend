package com.project.bluffball.domain.team.entity;

import com.project.bluffball.domain.team.enums.TeamJoinApplicationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 팀 가입 신청 엔터티.
 *
 * <p>승인제 팀({@code APPROVAL_REQUIRED})에 대한 PENDING 신청을 보관한다.</p>
 */
@Entity
@Table(name = "team_join_application")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamJoinApplication {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_join_application_id")
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    /** 신청자 유저 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private TeamJoinApplicationStatus status;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /** 승인/거부한 리더 유저 ID */
    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

    /**
     * PENDING 신청을 생성한다.
     *
     * @param teamId 팀 ID
     * @param userId 신청자 ID
     */
    public TeamJoinApplication(Long teamId, Long userId) {
        this.teamId = teamId;
        this.userId = userId;
        this.status = TeamJoinApplicationStatus.PENDING;
    }

    /**
     * 신청을 승인한다.
     *
     * @param reviewerUserId 승인한 리더 ID
     */
    public void approve(Long reviewerUserId) {
        requirePending();
        this.status = TeamJoinApplicationStatus.APPROVED;
        this.reviewedByUserId = reviewerUserId;
        this.reviewedAt = LocalDateTime.now(KST);
    }

    /**
     * 신청을 거부한다.
     *
     * @param reviewerUserId 거부한 리더 ID
     */
    public void reject(Long reviewerUserId) {
        requirePending();
        this.status = TeamJoinApplicationStatus.REJECTED;
        this.reviewedByUserId = reviewerUserId;
        this.reviewedAt = LocalDateTime.now(KST);
    }

    /**
     * 신청자가 신청을 취소한다.
     */
    public void cancel() {
        requirePending();
        this.status = TeamJoinApplicationStatus.CANCELLED;
        this.reviewedAt = LocalDateTime.now(KST);
    }

    /**
     * PENDING이 아니면 상태 변경을 막는다.
     */
    private void requirePending() {
        if (this.status != TeamJoinApplicationStatus.PENDING) {
            throw new IllegalStateException("대기 중인 가입 신청만 처리할 수 있습니다. status=" + this.status);
        }
    }

    @PrePersist
    private void prePersist() {
        this.requestedAt = LocalDateTime.now(KST);
        if (this.status == null) {
            this.status = TeamJoinApplicationStatus.PENDING;
        }
    }
}
