package com.project.bluffball.domain.auth.dto;

/**
 * 소셜 OAuth API에서 조회한 유저 식별 정보.
 *
 * <p>OAuth Client가 소셜 플랫폼 응답을 파싱한 뒤 Service에 전달하는 DTO.</p>
 */
public record SocialUserInfo(

        /** 소셜 플랫폼이 발급한 고유 유저 ID */
        String providerUserId
) {
}
