package com.project.bluffball.domain.card.dto.response;

import com.project.bluffball.domain.card.enums.ChangeDirection;
import com.project.bluffball.domain.card.enums.TimingEnhancement;
import com.project.bluffball.domain.game.enums.Timing;

/**
 * 유저 보유 구종 카드 인스턴스(강화 반영) 응답.
 *
 * @param userPitchCardId       보유 인스턴스 ID
 * @param cardId                마스터 구종 ID
 * @param name                  구종 이름
 * @param direction             변화 방향
 * @param baseChangeAmount      마스터 변화량
 * @param effectiveChangeAmount 실효 변화량
 * @param changeAmountEnhanced  변화량 강화 여부
 * @param baseTiming            마스터 타이밍
 * @param effectiveTiming       실효 타이밍
 * @param timingEnhancement     타이밍 강화
 * @param cost                  핸드 코스트
 */
public record UserPitchCardResponse(
        Long userPitchCardId,
        Long cardId,
        String name,
        ChangeDirection direction,
        int baseChangeAmount,
        int effectiveChangeAmount,
        boolean changeAmountEnhanced,
        Timing baseTiming,
        Timing effectiveTiming,
        TimingEnhancement timingEnhancement,
        int cost
) {
}
