package com.project.bluffball.domain.game.bot;

import com.project.bluffball.domain.game.enums.BotDifficulty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 난이도별 봇 구종(마스터) 덱 카탈로그.
 *
 * <p>TODO: 난이도별로 실제 마스터 구종 카드 ID 목록을 튜닝한다.
 * 현재 Showdown 인게임 드로우는 전체 마스터 풀을 사용하므로,
 * 이 카탈로그는 아직 매치 생성 경로에 강제되지 않는다.</p>
 */
@Component
public class BotDeckCatalog {

    /**
     * 난이도에 해당하는 마스터 구종 카드 ID 목록을 반환한다.
     *
     * @param difficulty 봇 난이도
     * @return 마스터 구종 ID 목록 (placeholder — 비어 있을 수 있음)
     */
    public List<Long> masterPitchCardIdsFor(BotDifficulty difficulty) {
        if (difficulty == null) {
            return List.of();
        }
        // TODO: 난이도별 덱 구성 (EASY/NORMAL/HARD)
        return switch (difficulty) {
            case EASY -> List.of();
            case NORMAL -> List.of();
            case HARD -> List.of();
        };
    }
}
