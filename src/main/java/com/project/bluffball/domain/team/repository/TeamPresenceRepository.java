package com.project.bluffball.domain.team.repository;

import com.project.bluffball.domain.team.redis.TeamPresence;
import org.springframework.data.repository.CrudRepository;

/**
 * 팀 멤버 프레즌스 Redis Repository.
 */
public interface TeamPresenceRepository extends CrudRepository<TeamPresence, String> {
}
