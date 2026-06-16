package com.project.bluffball.domain.auth.repository;

import com.project.bluffball.domain.auth.redis.RefreshToken;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
}
