package com.project.bluffball.domain.user.record.repository;

import com.project.bluffball.domain.user.record.entity.BatterRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 타자 성적 JPA Repository.
 */
public interface BatterRecordRepository extends JpaRepository<BatterRecord, Long> {

    /**
     * 유저의 타자 성적 목록을 조회한다.
     *
     * @param userId 유저 ID
     * @return 타자 기록 목록
     */
    List<BatterRecord> findByUserId(Long userId);
}
