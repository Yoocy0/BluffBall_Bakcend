package com.project.bluffball.domain.game.repository;

import com.project.bluffball.domain.game.redis.TurnResultSession;
import org.springframework.data.repository.CrudRepository;

public interface TurnResultSessionRepository extends CrudRepository<TurnResultSession, String> {
}
