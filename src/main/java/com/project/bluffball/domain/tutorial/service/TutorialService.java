package com.project.bluffball.domain.tutorial.service;

import com.project.bluffball.domain.card.dto.response.UserPitchCardResponse;
import com.project.bluffball.domain.card.service.usecase.reader.UserPitchCardReader;
import com.project.bluffball.domain.tutorial.dto.request.TutorialCompleteRequest;
import com.project.bluffball.domain.tutorial.dto.response.TutorialCompleteResponse;
import com.project.bluffball.domain.tutorial.dto.response.TutorialStatusResponse;
import com.project.bluffball.domain.tutorial.service.usecase.executor.TutorialCompleteExecutor;
import com.project.bluffball.domain.tutorial.service.usecase.reader.TutorialReader;
import com.project.bluffball.domain.tutorial.service.usecase.validator.TutorialCompleteValidator;
import com.project.bluffball.domain.user.service.usecase.reader.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 튜토리얼 완료 여부·시작 구종 지급 서비스.
 *
 * <p>설명·퀴즈·데모 플레이는 프론트에서 처리한다.</p>
 */
@Service
@RequiredArgsConstructor
public class TutorialService {

    /** 튜토리얼 Reader */
    private final TutorialReader tutorialReader;

    /** 완료 검증 */
    private final TutorialCompleteValidator tutorialCompleteValidator;

    /** 완료·지급 Executor */
    private final TutorialCompleteExecutor tutorialCompleteExecutor;

    /** 유저 Reader */
    private final UserReader userReader;

    /** 보유 구종 Reader */
    private final UserPitchCardReader userPitchCardReader;

    /**
     * 튜토리얼 완료 여부와 시작 구종 선택지를 조회한다.
     *
     * @param userId 유저 ID
     * @return 상태
     */
    public TutorialStatusResponse getStatus(Long userId) {
        return tutorialReader.getStatus(userId);
    }

    /**
     * 튜토리얼을 완료하고 시작 구종을 지급한다.
     *
     * @param userId  유저 ID
     * @param request 선택 구종 2개
     * @return 지급된 구종 목록
     */
    public TutorialCompleteResponse complete(Long userId, TutorialCompleteRequest request) {
        tutorialCompleteValidator.validateNotCompleted(userReader.isTutorialCompleted(userId));

        List<Long> selectedIds = request.selectedPitchCardIds();
        List<String> selectedNames = tutorialReader.getPitchNamesByIds(selectedIds);
        tutorialCompleteValidator.validateStarterSelection(selectedIds, selectedNames);

        Long fixedId = tutorialReader.getFixedStarterPitchCardId();
        List<Long> instanceIds = tutorialCompleteExecutor.completeAndGrant(userId, fixedId, selectedIds);

        List<UserPitchCardResponse> cards = instanceIds.stream()
                .map(instanceId -> userPitchCardReader.getResponseByInstance(userId, instanceId))
                .toList();
        return new TutorialCompleteResponse(cards);
    }
}
