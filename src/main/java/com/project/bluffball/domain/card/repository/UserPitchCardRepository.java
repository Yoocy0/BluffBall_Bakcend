package com.project.bluffball.domain.card.repository;

import com.project.bluffball.domain.card.entity.UserPitchCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 유저 보유 구종 카드 JPA Repository.
 */
public interface UserPitchCardRepository extends JpaRepository<UserPitchCard, Long> {

    /**
     * 유저·마스터 카드로 보유 여부를 조회한다.
     *
     * @param userId 유저 ID
     * @param cardId 마스터 구종 ID
     * @return Optional
     */
    Optional<UserPitchCard> findByUserIdAndCardId(Long userId, Long cardId);

    /**
     * 유저 보유 카드 전체 목록을 조회한다.
     *
     * @param userId 유저 ID
     * @return 보유 목록
     */
    List<UserPitchCard> findByUserIdOrderByCardIdAsc(Long userId);

    /**
     * 유저가 특정 마스터 카드를 보유했는지 확인한다.
     *
     * @param userId 유저 ID
     * @param cardId 마스터 구종 ID
     * @return 보유하면 true
     */
    boolean existsByUserIdAndCardId(Long userId, Long cardId);

    /**
     * 유저가 보유한 마스터 카드 ID 목록을 조회한다.
     *
     * @param userId 유저 ID
     * @param cardIds 조회할 마스터 ID 집합
     * @return 보유 중인 카드 목록
     */
    List<UserPitchCard> findByUserIdAndCardIdIn(Long userId, Collection<Long> cardIds);
}
