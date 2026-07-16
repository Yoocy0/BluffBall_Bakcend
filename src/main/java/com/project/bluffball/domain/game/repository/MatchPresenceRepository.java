package com.project.bluffball.domain.game.repository;

import com.project.bluffball.domain.game.redis.MatchPresence;
import org.springframework.data.repository.CrudRepository;

public interface MatchPresenceRepository extends CrudRepository<MatchPresence, String> {
}
