package com.project.bluffball.domain.team.enums;

/**
 * 팀 가입 정책.
 * DB에는 ordinal(0=OPEN, 1=APPROVAL_REQUIRED) 정수로 저장된다.
 */
public enum TeamJoinPolicy {
    /** 신청 즉시 가입 */
    OPEN,
    /** 리더 승인 후 가입 */
    APPROVAL_REQUIRED
}
