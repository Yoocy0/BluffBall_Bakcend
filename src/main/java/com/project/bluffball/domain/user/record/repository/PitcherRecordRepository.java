package com.project.bluffball.domain.user.record.repository;

import com.project.bluffball.domain.user.record.entity.PitcherRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 투수 성적 JPA Repository.
 */
public interface PitcherRecordRepository extends JpaRepository<PitcherRecord, Long> {

    /**
     * 유저의 투수 성적 목록을 조회한다.
     *
     * @param userId 유저 ID
     * @return 투수 기록 목록
     */
    List<PitcherRecord> findByUserId(Long userId);
}
