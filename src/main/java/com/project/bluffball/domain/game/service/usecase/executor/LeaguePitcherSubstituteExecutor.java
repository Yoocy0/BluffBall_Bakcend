package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.game.redis.MatchInfo;
import com.project.bluffball.domain.game.repository.MatchInfoRepository;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 리그 투수 교체 Executor.
 *
 * <p>신임 투수 핸드에서 dropCard를 제거하고, 셋업을 역할에 맞게 초기화한다.</p>
 */
@Component
@RequiredArgsConstructor
public class LeaguePitcherSubstituteExecutor {

    private final MatchInfoRepository matchInfoRepository;
    private final MatchInfoReader matchInfoReader;

    /**
     * 투수를 교체한다.
     *
     * <p>신임 투수(기존 타자)는 마운드만 맡고, 강판 투수는 신임의 타순 슬롯을 이어받아
     * 이후 공격 이닝에서 타석에 선다.</p>
     *
     * @param matchSessionId 매치 세션 ID
     * @param oldPitcherUserId 강판 투수
     * @param newPitcherUserId 신임 투수
     * @param dropCardId 신임 투수 핸드에서 제외할 카드
     * @return 신임 투수 userId
     */
    public Long execute(
            String matchSessionId,
            Long oldPitcherUserId,
            Long newPitcherUserId,
            Long dropCardId) {
        MatchInfo matchInfo = matchInfoReader.getById(matchSessionId);

        List<Long> hand = new ArrayList<>(matchInfo.getPlayerCardHand(newPitcherUserId));
        if (!hand.remove(dropCardId)) {
            throw new BadRequestException(
                    ErrorCode.GAME_PITCHER_SUBSTITUTE_INVALID,
                    "dropCardId not in hand. dropCardId=" + dropCardId);
        }
        matchInfo.setPlayerCardHand(newPitcherUserId, hand);

        // 타순 슬롯 교환: 강판 투수 ↔ 신임(타자) — Compact 전담 투수도 타석 진입
        matchInfo.swapDefendingBattingOrderOnPitcherSubstitute(oldPitcherUserId, newPitcherUserId);

        // 신임 투수 → 투수 셋업 재제출
        matchInfo.clearPitcherSetupNumbers(newPitcherUserId);
        // 강판 투수 → 타자 셋업 재제출 (이제 타순 멤버)
        matchInfo.clearBatterSetupNumbers(oldPitcherUserId);

        matchInfo.substitutePitcher(newPitcherUserId);
        matchInfoRepository.save(matchInfo);
        return newPitcherUserId;
    }
}
