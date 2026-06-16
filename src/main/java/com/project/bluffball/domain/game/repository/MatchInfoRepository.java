package com.project.bluffball.domain.game.repository;

import com.project.bluffball.domain.game.redis.MatchInfo;
import org.springframework.data.repository.CrudRepository;

public interface MatchInfoRepository extends CrudRepository<MatchInfo, String> {
}
