package com.project.bluffball.domain.tutorial.service.usecase.reader;

import com.project.bluffball.domain.card.entity.PitchCard;
import com.project.bluffball.domain.card.repository.PitchCardRepository;
import com.project.bluffball.domain.tutorial.TutorialConstants;
import com.project.bluffball.domain.tutorial.dto.response.TutorialStarterPitchOption;
import com.project.bluffball.domain.tutorial.dto.response.TutorialStatusResponse;
import com.project.bluffball.domain.user.service.usecase.reader.UserReader;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 튜토리얼 완료 상태·시작 구종 옵션 읽기 Reader.
 */
@Component
@RequiredArgsConstructor
public class TutorialReader {

    /** 유저 Reader */
    private final UserReader userReader;

    /** 마스터 구종 Repository */
    private final PitchCardRepository pitchCardRepository;

    /**
     * 튜토리얼 완료 여부와 시작 구종 선택지를 반환한다.
     *
     * @param userId 유저 ID
     * @return 상태 응답
     */
    public TutorialStatusResponse getStatus(Long userId) {
        boolean completed = userReader.isTutorialCompleted(userId);
        PitchCard fixed = requireByName(TutorialConstants.FIXED_STARTER_PITCH_NAME);
        return new TutorialStatusResponse(
                completed,
                new TutorialStarterPitchOption(fixed.getId(), fixed.getName()),
                resolveSelectableOptions(),
                TutorialConstants.SELECTABLE_STARTER_COUNT);
    }

    /**
     * 선택 구종 ID에 대응하는 이름을 순서대로 반환한다.
     *
     * @param cardIds 마스터 구종 ID
     * @return 구종 이름 목록
     */
    public List<String> getPitchNamesByIds(List<Long> cardIds) {
        Map<Long, PitchCard> byId = pitchCardRepository.findAllByIds(cardIds).stream()
                .collect(Collectors.toMap(PitchCard::getId, Function.identity()));
        List<String> names = new ArrayList<>();
        for (Long cardId : cardIds) {
            PitchCard card = byId.get(cardId);
            if (card == null) {
                throw new NotFoundException(ErrorCode.PITCH_CARD_NOT_FOUND, "cardId=" + cardId);
            }
            names.add(card.getName());
        }
        return names;
    }

    /**
     * 고정 지급 구종 ID를 반환한다.
     *
     * @return 포심 패스트볼 cardId
     */
    public Long getFixedStarterPitchCardId() {
        return requireByName(TutorialConstants.FIXED_STARTER_PITCH_NAME).getId();
    }

    private List<TutorialStarterPitchOption> resolveSelectableOptions() {
        Map<String, PitchCard> byName = pitchCardRepository
                .findByNameIn(TutorialConstants.SELECTABLE_STARTER_PITCH_NAMES)
                .stream()
                .collect(Collectors.toMap(PitchCard::getName, Function.identity()));
        List<TutorialStarterPitchOption> options = new ArrayList<>();
        for (String name : TutorialConstants.SELECTABLE_STARTER_PITCH_NAMES) {
            PitchCard card = byName.get(name);
            if (card == null) {
                throw new NotFoundException(ErrorCode.PITCH_CARD_NOT_FOUND, "name=" + name);
            }
            options.add(new TutorialStarterPitchOption(card.getId(), card.getName()));
        }
        return options;
    }

    private PitchCard requireByName(String name) {
        return pitchCardRepository.findByName(name)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PITCH_CARD_NOT_FOUND, "name=" + name));
    }
}
