package com.project.bluffball.domain.game.repository;

import com.project.bluffball.domain.game.entity.InningLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InningLogRepository extends JpaRepository<InningLog, Long> {
}
