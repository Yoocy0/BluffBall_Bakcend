package com.project.bluffball.domain.store;

import java.time.ZoneId;

/**
 * 상점 상수 (상시 구매).
 */
public final class StoreConstants {

    /** 상점 날짜·시각 기준 타임존 */
    public static final ZoneId STORE_ZONE = ZoneId.of("Asia/Seoul");

    /** 구종(기본본) 구매 가격 */
    public static final long DEFAULT_PITCH_PRICE = 100L;

    /** 강화 카드 1장 구매 가격 */
    public static final long DEFAULT_ENHANCEMENT_PRICE = 80L;

    private StoreConstants() {
    }
}
