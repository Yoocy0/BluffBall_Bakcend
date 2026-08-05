package com.project.bluffball.domain.user.dto.response;

import java.time.LocalDateTime;

/**
 * 유저 기본 프로필 응답 (닉네임·재화 등).
 *
 * <p>전적(타자/투수 기록)은 {@link UserRecordsResponse}로 별도 조회한다.</p>
 */
public record UserProfileResponse(
        Long userId,
        String nickname,
        long currency,
        boolean nicknameChangeFree,
        boolean tutorialCompleted,
        LocalDateTime createdAt
) {
}
