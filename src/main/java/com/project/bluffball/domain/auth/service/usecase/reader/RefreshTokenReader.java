package com.project.bluffball.domain.auth.service.usecase.reader;

import com.project.bluffball.domain.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Refresh Token 읽기 전담 Reader (usecase/reader 계층).
 *
 * <p>Redis에서 토큰 존재 여부 및 userId를 조회한다.</p>
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenReader {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Refresh Token으로 유저 ID 조회 (Service ✅).
     *
     * @return 토큰이 없거나 만료되었으면 empty
     */
    public Optional<Long> findUserIdByToken(String refreshToken) {
        return refreshTokenRepository.findById(refreshToken)
                .map(token -> token.getUserId());
    }
}
