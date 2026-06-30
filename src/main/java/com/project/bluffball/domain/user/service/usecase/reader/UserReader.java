package com.project.bluffball.domain.user.service.usecase.reader;

import com.project.bluffball.domain.user.entity.User;
import com.project.bluffball.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 유저 읽기 전담 Reader (usecase/reader 계층).
 *
 * <p>Service는 이 클래스의 원시값·문자열 반환 메서드만 호출한다.</p>
 */
@Component
@RequiredArgsConstructor
public class UserReader {

    private final UserRepository userRepository;

    /** Executor·Reader 내부 전용 — Service에서 호출 금지 */
    User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "유저를 찾을 수 없습니다. userId=" + userId));
    }

    /** 계정 활성 여부 (Service ✅) */
    public boolean isActive(Long userId) {
        return getById(userId).isActive();
    }

    /** JWT claim에 담을 권한명 (Service ✅) */
    public String getRoleName(Long userId) {
        return getById(userId).getRole().name();
    }
}
