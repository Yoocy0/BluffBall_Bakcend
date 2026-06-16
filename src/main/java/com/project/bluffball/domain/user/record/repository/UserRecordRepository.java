package com.project.bluffball.domain.user.record.repository;

import com.project.bluffball.domain.user.record.entity.UserRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRecordRepository extends JpaRepository<UserRecord, Long> {
}
