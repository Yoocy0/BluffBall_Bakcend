package com.project.bluffball.domain.auth.redis;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

/**
 * Redis 저장용 리프레시 토큰 객체.
 * JPA 엔터티가 아니며, Spring Data Redis의 @RedisHash로 관리된다.
 *
 * <p>Redis 저장 구조</p>
 * <pre>
 * KEY : RefreshToken:{token 문자열}
 * FIELD: userId → 유저 ID
 * FIELD: ttl    → 잔여 만료 시간(초)
 * TTL : 1209600초 (14일) 후 Redis가 자동 삭제
 * </pre>
 */
@RedisHash(value = "RefreshToken", timeToLive = 1209600)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    /** Redis Key — 발급된 Refresh Token 문자열 자체를 식별자로 사용 */
    @Id
    private String id;

    /** 이 토큰을 발급받은 유저의 고유 ID */
    private Long userId;

    /** 잔여 만료 시간 (초 단위) — 동적 TTL 조회 및 재발급 시 활용 */
    @TimeToLive
    private Long ttl;

    @Builder
    public RefreshToken(String id, Long userId) {
        this.id = id;
        this.userId = userId;
        this.ttl = 1209600L;
    }
}
