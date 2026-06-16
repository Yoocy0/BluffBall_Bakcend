package com.project.bluffball.domain.user.repository;

import com.project.bluffball.domain.user.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
}
