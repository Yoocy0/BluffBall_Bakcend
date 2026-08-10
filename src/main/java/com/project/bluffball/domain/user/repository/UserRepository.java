package com.project.bluffball.domain.user.repository;

import com.project.bluffball.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /** 닉네임으로 유저 조회 — 봇 풀 find-or-create 등 */
    Optional<User> findByNickname(String nickname);
}
