package com.project.bluffball.domain.user.record.repository;

import com.project.bluffball.domain.user.record.entity.PitcherRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PitcherRecordRepository extends JpaRepository<PitcherRecord, Long> {
}
