package com.project.bluffball.domain.card.service.usecase.executor;

import com.project.bluffball.domain.card.entity.UserPitchCard;
import com.project.bluffball.domain.card.enums.TimingEnhancement;
import com.project.bluffball.domain.card.repository.PitchCardRepository;
import com.project.bluffball.domain.card.repository.UserPitchCardRepository;
import com.project.bluffball.domain.card.service.usecase.reader.UserPitchCardReader;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 유저 구종 카드 획득·강화 쓰기 Executor.
 */
@Component
@RequiredArgsConstructor
public class UserPitchCardExecutor {

    /** 유저 보유 Repository */
    private final UserPitchCardRepository userPitchCardRepository;

    /** 마스터 구종 Repository */
    private final PitchCardRepository pitchCardRepository;

    /** 유저 구종 Reader */
    private final UserPitchCardReader userPitchCardReader;

    /**
     * 구종 카드를 획득한다 (미강화).
     *
     * @param userId 유저 ID
     * @param cardId 마스터 구종 ID
     * @return 마스터 카드 ID
     */
    @Transactional
    public Long acquire(Long userId, Long cardId) {
        requireMasterExists(cardId);
        userPitchCardRepository.save(new UserPitchCard(userId, cardId));
        return cardId;
    }

    /**
     * 미보유 마스터 구종을 모두 획득한다.
     *
     * @param userId 유저 ID
     * @return 새로 획득한 마스터 카드 ID 목록
     */
    @Transactional
    public List<Long> acquireAllMissing(Long userId) {
        List<Long> acquired = new ArrayList<>();
        for (Long cardId : pitchCardRepository.findAllIds()) {
            if (!userPitchCardRepository.existsByUserIdAndCardId(userId, cardId)) {
                userPitchCardRepository.save(new UserPitchCard(userId, cardId));
                acquired.add(cardId);
            }
        }
        return acquired;
    }

    /**
     * 변화량 강화를 적용한다.
     *
     * @param userId 유저 ID
     * @param cardId 마스터 구종 ID
     * @return 마스터 카드 ID
     */
    @Transactional
    public Long enhanceChangeAmount(Long userId, Long cardId) {
        UserPitchCard owned = userPitchCardReader.getByUserIdAndCardId(userId, cardId);
        owned.enhanceChangeAmount();
        return cardId;
    }

    /**
     * 타이밍 강화를 적용한다.
     *
     * @param userId 유저 ID
     * @param cardId 마스터 구종 ID
     * @param enhancement FASTER 또는 SLOWER
     * @return 마스터 카드 ID
     */
    @Transactional
    public Long enhanceTiming(Long userId, Long cardId, TimingEnhancement enhancement) {
        UserPitchCard owned = userPitchCardReader.getByUserIdAndCardId(userId, cardId);
        owned.enhanceTiming(enhancement);
        return cardId;
    }

    private void requireMasterExists(Long cardId) {
        if (!pitchCardRepository.existsById(cardId)) {
            throw new NotFoundException(ErrorCode.PITCH_CARD_NOT_FOUND, "cardId=" + cardId);
        }
    }
}
