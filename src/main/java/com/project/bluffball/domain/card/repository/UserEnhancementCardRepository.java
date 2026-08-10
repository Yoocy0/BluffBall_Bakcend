package com.project.bluffball.domain.card.repository;

import com.project.bluffball.domain.card.entity.UserEnhancementCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 유저 강화 카드 보유 Repository.
 */
public interface UserEnhancementCardRepository extends JpaRepository<UserEnhancementCard, Long> {

    /**
     * 유저·강화 카드로 보유 행을 조회한다.
     *
     * @param userId            유저 ID
     * @param enhancementCardId 마스터 강화 카드 ID
     * @return Optional
     */
    Optional<UserEnhancementCard> findByUserIdAndEnhancementCardId(Long userId, Long enhancementCardId);

    /**
     * 유저 보유 목록 (수량 0 포함 가능).
     *
     * @param userId 유저 ID
     * @return 보유 목록
     */
    List<UserEnhancementCard> findByUserIdOrderByEnhancementCardIdAsc(Long userId);

    /**
     * 보유 행 존재 여부.
     *
     * @param userId            유저 ID
     * @param enhancementCardId 마스터 ID
     * @return 존재하면 true
     */
    boolean existsByUserIdAndEnhancementCardId(Long userId, Long enhancementCardId);
}
