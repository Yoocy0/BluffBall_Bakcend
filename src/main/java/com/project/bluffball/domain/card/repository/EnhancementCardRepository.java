package com.project.bluffball.domain.card.repository;

import com.project.bluffball.domain.card.entity.EnhancementCard;
import com.project.bluffball.domain.card.enums.EnhancementEffect;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 강화 카드 마스터 Repository.
 */
public interface EnhancementCardRepository extends JpaRepository<EnhancementCard, Long> {

    /**
     * 이름 존재 여부.
     *
     * @param name 카드 이름
     * @return 존재하면 true
     */
    boolean existsByName(String name);

    /**
     * 효과로 조회한다.
     *
     * @param effect 효과
     * @return Optional
     */
    Optional<EnhancementCard> findByEffect(EnhancementEffect effect);

    /**
     * 효과 존재 여부.
     *
     * @param effect 효과
     * @return 존재하면 true
     */
    boolean existsByEffect(EnhancementEffect effect);
}
