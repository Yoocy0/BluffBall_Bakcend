package com.project.bluffball.domain.user.enums;

/**
 * 유저 권한 등급.
 * DB에는 ordinal(0, 1) 정수로 저장된다.
 */
public enum UserRole {
    USER,   // 0 - 일반 유저
    ADMIN   // 1 - 관리자
}
