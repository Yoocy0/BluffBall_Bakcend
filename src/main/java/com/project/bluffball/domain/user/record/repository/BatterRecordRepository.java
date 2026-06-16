package com.project.bluffball.domain.user.record.repository;

import com.project.bluffball.domain.user.record.entity.BatterRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatterRecordRepository extends JpaRepository<BatterRecord, Long> {
}
