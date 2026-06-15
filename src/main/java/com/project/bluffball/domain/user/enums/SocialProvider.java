package com.project.bluffball.domain.user.enums;

/**
 * 소셜 로그인 제공자 구분.
 * DB에는 ordinal(0, 1) 정수로 저장된다.
 */
public enum SocialProvider {
    KAKAO,  // 0 - 카카오 로그인
    GOOGLE  // 1 - 구글 로그인
}
