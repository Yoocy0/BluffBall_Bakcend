package com.project.bluffball.domain.auth.service.usecase.executor;

import com.project.bluffball.domain.auth.redis.RefreshToken;
import com.project.bluffball.domain.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Refresh Token 쓰기 전담 Executor (usecase/executor 계층).
 *
 * <p>Redis에 Refresh Token을 발급·교체(Rotation)한다.</p>
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenExecutor {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 새 Refresh Token을 발급하고 Redis에 저장한다.
     *
     * @return 발급된 Refresh Token 문자열
     */
    public String issue(Long userId) {
        String token = UUID.randomUUID().toString();
        refreshTokenRepository.save(
                RefreshToken.builder()
                        .id(token)
                        .userId(userId)
                        .build());
        return token;
    }

    /**
     * 기존 Refresh Token을 무효화하고 새 토큰을 발급한다 (Rotation).
     *
     * @param oldToken 교체 대상 Refresh Token
     * @return 새로 발급된 Refresh Token 문자열
     */
    public String rotate(String oldToken, Long userId) {
        refreshTokenRepository.deleteById(oldToken);
        return issue(userId);
    }
}
