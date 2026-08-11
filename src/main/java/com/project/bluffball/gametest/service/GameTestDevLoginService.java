package com.project.bluffball.gametest.service;

import com.project.bluffball.domain.auth.dto.response.LoginResponse;
import com.project.bluffball.domain.auth.infrastructure.jwt.JwtTokenProvider;
import com.project.bluffball.domain.auth.service.usecase.executor.RefreshTokenExecutor;
import com.project.bluffball.domain.card.service.UserPitchCardService;
import com.project.bluffball.domain.user.entity.User;
import com.project.bluffball.domain.user.repository.UserRepository;
import com.project.bluffball.domain.user.service.usecase.reader.UserReader;
import com.project.bluffball.gametest.GameTestDevUserConstants;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게임 테스트 개발자 PIN 로그인 서비스.
 *
 * <p>고정 닉네임 계정을 찾거나 만들고, 전 구종 기본본을 보장한 뒤 JWT를 발급한다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameTestDevLoginService {

    private final UserRepository userRepository;
    private final UserReader userReader;
    private final UserPitchCardService userPitchCardService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenExecutor refreshTokenExecutor;

    /**
     * PIN 검증 후 개발자 테스트 계정으로 로그인한다.
     *
     * @param pin 4자리 PIN
     * @return OAuth와 동일한 LoginResponse
     */
    @Transactional
    public LoginResponse login(String pin) {
        if (!GameTestDevUserConstants.PIN.equals(pin == null ? null : pin.trim())) {
            throw new BadRequestException(ErrorCode.INVALID_REQUEST, "개발자 PIN이 올바르지 않습니다.");
        }

        User user = ensureDevUser();
        // 신규 마스터 구종이 추가돼도 로그인 시 다시 맞춘다.
        userPitchCardService.acquireAllMissing(user.getId());

        String roleName = userReader.getRoleName(user.getId());
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), roleName);
        String refreshToken = refreshTokenExecutor.issue(user.getId());

        log.info("game-test dev login userId={} nickname={}", user.getId(), user.getNickname());
        return new LoginResponse(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpiresInSeconds(),
                false);
    }

    /**
     * 개발자 테스트 유저를 찾거나 생성한다. 튜토리얼은 완료 처리한다.
     *
     * @return 영속 User
     */
    @Transactional
    public User ensureDevUser() {
        User user = userRepository.findByNickname(GameTestDevUserConstants.NICKNAME)
                .orElseGet(() -> {
                    User created = userRepository.save(new User(GameTestDevUserConstants.NICKNAME));
                    log.info("game-test dev user created userId={}", created.getId());
                    return created;
                });
        if (!user.isTutorialCompleted()) {
            user.completeTutorial();
        }
        return user;
    }
}
