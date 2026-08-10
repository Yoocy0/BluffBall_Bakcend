package com.project.bluffball.domain.tutorial.service.usecase.executor;

import com.project.bluffball.domain.card.service.usecase.executor.UserPitchCardExecutor;
import com.project.bluffball.domain.user.entity.User;
import com.project.bluffball.domain.user.repository.UserRepository;
import com.project.bluffball.domain.user.service.usecase.reader.UserReader;
import com.project.bluffball.global.exception.ConflictException;
import com.project.bluffball.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 튜토리얼 완료·시작 구종 지급 Executor.
 */
@Component
@RequiredArgsConstructor
public class TutorialCompleteExecutor {

    /** 유저 Reader */
    private final UserReader userReader;

    /** 유저 Repository */
    private final UserRepository userRepository;

    /** 구종 획득 Executor */
    private final UserPitchCardExecutor userPitchCardExecutor;

    /**
     * 튜토리얼을 완료하고 시작 구종을 지급한다.
     *
     * @param userId              유저 ID
     * @param fixedPitchCardId     포심 패스트볼 마스터 ID
     * @param selectedPitchCardIds 선택한 구종 마스터 ID 2개
     * @return 지급·보유 인스턴스 ID (포심 + 선택 2)
     */
    @Transactional
    public List<Long> completeAndGrant(
            Long userId,
            Long fixedPitchCardId,
            List<Long> selectedPitchCardIds) {
        User user = userReader.getById(userId);
        try {
            user.completeTutorial();
        } catch (IllegalStateException ex) {
            throw new ConflictException(ErrorCode.TUTORIAL_ALREADY_COMPLETED);
        }
        userRepository.save(user);

        List<Long> grantMasterIds = new ArrayList<>();
        grantMasterIds.add(fixedPitchCardId);
        grantMasterIds.addAll(selectedPitchCardIds);
        return userPitchCardExecutor.acquireIfMissing(userId, grantMasterIds);
    }
}
