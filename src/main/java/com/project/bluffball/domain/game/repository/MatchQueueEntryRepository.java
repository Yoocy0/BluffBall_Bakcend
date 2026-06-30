package com.project.bluffball.domain.game.repository;

import com.project.bluffball.domain.game.redis.MatchQueueEntry;
import org.springframework.data.repository.CrudRepository;

/**
 * {@link MatchQueueEntry} Redis Hash CRUD Repository.
 *
 * <p>Key: {@code MatchQueueEntry:{userId}}</p>
 */
public interface MatchQueueEntryRepository extends CrudRepository<MatchQueueEntry, Long> {
}
