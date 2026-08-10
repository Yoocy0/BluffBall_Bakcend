package com.project.bluffball.domain.card.repository;

import com.project.bluffball.domain.card.entity.UserPitchCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 유저 보유 구종 카드 인스턴스 Repository.
 */
public interface UserPitchCardRepository extends JpaRepository<UserPitchCard, Long> {

    /**
     * 인스턴스 ID·유저로 조회한다.
     *
     * @param id     인스턴스 ID
     * @param userId 유저 ID
     * @return Optional
     */
    Optional<UserPitchCard> findByIdAndUserId(Long id, Long userId);

    /**
     * 유저의 특정 마스터 구종 인스턴스 목록.
     *
     * @param userId 유저 ID
     * @param cardId 마스터 구종 ID
     * @return 인스턴스 목록
     */
    List<UserPitchCard> findByUserIdAndCardId(Long userId, Long cardId);

    /**
     * 유저 보유 전체 (마스터 ID·인스턴스 ID 순).
     *
     * @param userId 유저 ID
     * @return 보유 목록
     */
    List<UserPitchCard> findByUserIdOrderByCardIdAscIdAsc(Long userId);

    /**
     * 마스터 구종 인스턴스 존재 여부.
     *
     * @param userId 유저 ID
     * @param cardId 마스터 ID
     * @return 1장 이상이면 true
     */
    boolean existsByUserIdAndCardId(Long userId, Long cardId);

    /**
     * 유저 인스턴스 ID 목록으로 조회한다.
     *
     * @param userId 유저 ID
     * @param ids    인스턴스 ID
     * @return 보유 인스턴스
     */
    List<UserPitchCard> findByUserIdAndIdIn(Long userId, Collection<Long> ids);

    /**
     * 유저·마스터 집합으로 인스턴스를 조회한다.
     *
     * @param userId  유저 ID
     * @param cardIds 마스터 ID 집합
     * @return 인스턴스 목록
     */
    List<UserPitchCard> findByUserIdAndCardIdIn(Long userId, Collection<Long> cardIds);
}
