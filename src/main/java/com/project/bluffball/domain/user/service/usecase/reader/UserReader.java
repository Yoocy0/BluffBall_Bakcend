package com.project.bluffball.domain.user.service.usecase.reader;

import com.project.bluffball.domain.user.entity.User;
import com.project.bluffball.domain.user.repository.UserRepository;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 유저 읽기 전담 Reader (usecase/reader 계층).
 */
@Component
@RequiredArgsConstructor
public class UserReader {

    private final UserRepository userRepository;

    User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND, "userId=" + userId));
    }

    /**
     * 계정 활성 여부를 반환한다.
     *
     * @param userId 유저 ID
     * @return 활성이면 true
     */
    public boolean isActive(Long userId) {
        return getById(userId).isActive();
    }

    /**
     * 유저 권한 이름을 반환한다.
     *
     * @param userId 유저 ID
     * @return 권한 enum 이름
     */
    public String getRoleName(Long userId) {
        return getById(userId).getRole().name();
    }

    /**
     * 유저 닉네임을 반환한다.
     *
     * @param userId 유저 ID
     * @return 닉네임
     */
    public String getNickname(Long userId) {
        return getById(userId).getNickname();
    }

    /**
     * 유저 보유 재화를 반환한다.
     *
     * @param userId 유저 ID
     * @return 재화 잔액
     */
    public long getCurrency(Long userId) {
        return getById(userId).getCurrency();
    }
}
