package com.project.bluffball.domain.game.bot;

import com.project.bluffball.domain.tutorial.TutorialConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * 봇 EASY 난이도용 기본 덱(튜토리얼 스타터 전체 4장).
 *
 * <p>고정 지급 1장 + 선택 가능 3장 이름을 {@link TutorialConstants}에서 재사용한다.</p>
 */
public final class BotBasicDeck {

    /**
     * 기본 덱 구종 이름 — 포심 패스트볼, 커브, 슬라이더, 포크.
     */
    public static final List<String> PITCH_NAMES;

    static {
        List<String> names = new ArrayList<>(1 + TutorialConstants.SELECTABLE_STARTER_PITCH_NAMES.size());
        names.add(TutorialConstants.FIXED_STARTER_PITCH_NAME);
        names.addAll(TutorialConstants.SELECTABLE_STARTER_PITCH_NAMES);
        PITCH_NAMES = List.copyOf(names);
    }

    private BotBasicDeck() {
    }
}
