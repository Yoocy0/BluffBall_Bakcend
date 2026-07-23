package com.project.bluffball.domain.team.enums;

/**
 * 팀 내 역할.
 * DB에는 ordinal(0~1) 정수로 저장된다.
 */
public enum TeamMemberRole {
    LEADER,     // 0 - 팀장
    MEMBER      // 1 - 팀원
}
