package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.card.service.usecase.reader.UserPitchCardReader;
import com.project.bluffball.domain.game.config.GameModeRule;
import com.project.bluffball.domain.game.redis.MatchInfo;
import com.project.bluffball.domain.game.repository.MatchInfoRepository;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 초기 카드 패 뽑기 실행 및 Redis 저장 전담 컴포넌트.
 *
 * <p>매치 참가자 전원에게 모드별 핸드 장수만큼 카드를 지급한다.
 * 사람 참가자는 보유 {@code UserPitchCard} 인스턴스 ID 풀에서 뽑고,
 * 연습 봇은 매치에 저장된 {@code botDrawPool}을 사용한다.</p>
 */
@Component
@RequiredArgsConstructor
public class CardHandDrawExecutor {

    private final MatchInfoRepository matchInfoRepository;
    private final MatchInfoReader matchInfoReader;
    private final UserPitchCardReader userPitchCardReader;
    private final CardHandDrawer cardHandDrawer;
    private final GameModeRule gameModeRule;

    /**
     * 모든 참가자에게 카드 패를 뽑아 저장한다.
     *
     * <p>사람 측 핸드에는 보유 인스턴스 ID가 저장되어 강화가 반영된다.
     * 보유 인스턴스 수가 핸드 장수보다 적으면 {@code GAME_CARD_POOL_INSUFFICIENT}가 발생한다.</p>
     *
     * @param matchSessionId 매치 세션 ID
     * @return 참가자별 뽑힌 카드 ID 목록 (순서 = participantUserIds)
     */
    public List<List<Long>> executeForAllParticipants(String matchSessionId) {
        MatchInfo matchInfo = matchInfoReader.getById(matchSessionId);

        int handSize = gameModeRule.getHandSize(matchInfo.getGameMode());
        List<Long> participantIds = matchInfo.getParticipantUserIds();

        matchInfo.clearMulliganPhase();

        List<List<Long>> allHands = new ArrayList<>();
        for (Long userId : participantIds) {
            List<Long> pool = resolveDrawPool(matchInfo, userId);
            List<Long> drawnIds = cardHandDrawer.draw(pool, handSize);
            matchInfo.setPlayerCardHand(userId, drawnIds);
            allHands.add(drawnIds);
        }

        matchInfoRepository.save(matchInfo);
        return allHands;
    }

    /**
     * 참가자별 드로우 풀을 고른다.
     *
     * <p>연습 봇이면 저장된 botDrawPool, 아니면 해당 유저 보유 인스턴스 ID 전체.</p>
     *
     * @param matchInfo 매치
     * @param userId    참가자
     * @return 드로우 풀 (인스턴스 ID)
     */
    private List<Long> resolveDrawPool(MatchInfo matchInfo, Long userId) {
        if (matchInfo.isPracticeBotUser(userId) && matchInfo.hasBotDrawPool()) {
            return matchInfo.getBotDrawPool();
        }
        return userPitchCardReader.findOwnedInstanceIds(userId);
    }
}
