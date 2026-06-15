package com.project.bluffball.domain.user.record.enums;

/**
 * 성적 기록 타입 구분.
 * user_record 테이블의 record_type_code 컬럼과 매핑되는 식별자 참조용 Enum.
 *   PITCHER → record_type_code = 1
 *   BATTER  → record_type_code = 2
 */
public enum RecordType {
    PITCHER,    // record_type_code: 1
    BATTER      // record_type_code: 2
}
