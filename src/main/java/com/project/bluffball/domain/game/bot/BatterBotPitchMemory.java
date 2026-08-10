package com.project.bluffball.domain.game.bot;

import com.project.bluffball.domain.game.dto.response.CardInfo;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 타자 봇이 한 매치 동안 확인한 구종(마스터 ID) 기억.
 *
 * <p>{@code seen.size() < maxKnown}이면 전체 카탈로그를 고려하고,
 * {@code seen.size() >= maxKnown}이면 확인된 구종만 고려한다.</p>
 */
public final class BatterBotPitchMemory {

    /** 서로 다른 구종 확인이 이 값 미만이면 소극 모드 */
    public static final int PASSIVE_UNTIL_DISTINCT_SEEN = 2;

    /** 확인된 마스터 구종 ID (삽입 순서 유지) */
    private final Set<Long> seenMasterPitchIds = new LinkedHashSet<>();

    /**
     * 확인된 마스터 구종을 기록한다.
     *
     * @param masterPitchId 마스터 구종 ID
     */
    public void remember(Long masterPitchId) {
        if (masterPitchId == null) {
            return;
        }
        seenMasterPitchIds.add(masterPitchId);
    }

    /**
     * 아직 구종 정보가 없는지.
     *
     * @return seen이 비었으면 true
     * @deprecated 소극 모드는 {@link #isPassive()}를 사용
     */
    @Deprecated
    public boolean isEmpty() {
        return seenMasterPitchIds.isEmpty();
    }

    /**
     * 소극 모드인지 — 확인된 서로 다른 구종이 {@link #PASSIVE_UNTIL_DISTINCT_SEEN} 미만.
     *
     * @return {@code size() < PASSIVE_UNTIL_DISTINCT_SEEN}이면 true
     */
    public boolean isPassive() {
        return seenMasterPitchIds.size() < PASSIVE_UNTIL_DISTINCT_SEEN;
    }

    /**
     * 확인된 구종 수.
     *
     * @return distinct master 개수
     */
    public int size() {
        return seenMasterPitchIds.size();
    }

    /**
     * 확인된 마스터 ID 스냅샷.
     *
     * @return 수정 불가 집합
     */
    public Set<Long> seenMasterPitchIds() {
        return Collections.unmodifiableSet(seenMasterPitchIds);
    }

    /**
     * 판단용 고려 풀을 만든다.
     *
     * <p>확인 구종이 {@code maxKnown} 미만이면 카탈로그 전체,
     * 이상이면 확인된 마스터만. 제한 결과가 비면 카탈로그로 폴백.</p>
     *
     * @param catalog  전체 마스터 CardInfo
     * @param maxKnown 핸드 장수({@code GameModeRule#getHandSize})
     * @return 고려 풀
     */
    public List<CardInfo> considerationPool(List<CardInfo> catalog, int maxKnown) {
        if (catalog == null || catalog.isEmpty()) {
            return List.of();
        }
        if (seenMasterPitchIds.size() < maxKnown) {
            return List.copyOf(catalog);
        }
        List<CardInfo> restricted = catalog.stream()
                .filter(c -> c != null && seenMasterPitchIds.contains(c.cardId()))
                .toList();
        return restricted.isEmpty() ? List.copyOf(catalog) : restricted;
    }

    /**
     * 기억을 초기화한다(매치 시작·종료 시).
     */
    public void clear() {
        seenMasterPitchIds.clear();
    }
}
