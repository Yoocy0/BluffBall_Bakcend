package com.project.bluffball.domain.tutorial;

import java.util.List;
import java.util.Set;

/**
 * 튜토리얼 시작 구종 지급 상수.
 */
public final class TutorialConstants {

    /** 튜토리얼 완료 시 고정 지급 구종 이름 */
    public static final String FIXED_STARTER_PITCH_NAME = "포심 패스트볼";

    /** 튜토리얼 완료 시 2장 선택 가능한 구종 이름 */
    public static final List<String> SELECTABLE_STARTER_PITCH_NAMES = List.of(
            "커브",
            "슬라이더",
            "포크"
    );

    /** 선택 가능 구종 이름 Set */
    public static final Set<String> SELECTABLE_STARTER_PITCH_NAME_SET =
            Set.copyOf(SELECTABLE_STARTER_PITCH_NAMES);

    /** 선택해야 하는 구종 수 */
    public static final int SELECTABLE_STARTER_COUNT = 2;

    private TutorialConstants() {
    }
}
