package com.project.bluffball.domain.game.repository;

import com.project.bluffball.domain.game.redis.GameState;
import org.springframework.data.repository.CrudRepository;

public interface GameStateRepository extends CrudRepository<GameState, String> {
}
