package com.project.bluffball.domain.store;

import java.time.ZoneId;

/**
 * 상점·일일 구종 오퍼 상수.
 */
public final class StoreConstants {

    /** 상점 날짜 기준 타임존 */
    public static final ZoneId STORE_ZONE = ZoneId.of("Asia/Seoul");

    /** 하루 노출 구종 슬롯 수 */
    public static final int DAILY_OFFER_COUNT = 3;

    /** 구종 기본 구매 가격 (재화) */
    public static final long DEFAULT_PITCH_PRICE = 100L;

    private StoreConstants() {
    }
}
