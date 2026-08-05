package com.project.bluffball.domain.team.enums;

/**
 * 팀 가입 신청 상태.
 * DB에는 ordinal(0=PENDING …) 정수로 저장된다.
 */
public enum TeamJoinApplicationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}
