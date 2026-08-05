package com.project.bluffball.domain.team.enums;

/**
 * 팀 가입 요청 처리 결과.
 */
public enum TeamJoinOutcome {
    /** 즉시 멤버로 가입됨 */
    JOINED,
    /** 승인 대기 신청이 생성됨 */
    APPLICATION_SUBMITTED
}
