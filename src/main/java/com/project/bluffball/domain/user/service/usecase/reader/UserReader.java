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

    public boolean isActive(Long userId) {
        return getById(userId).isActive();
    }

    public String getRoleName(Long userId) {
        return getById(userId).getRole().name();
    }
}
