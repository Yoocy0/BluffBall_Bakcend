package com.project.bluffball.domain.store.repository;

import com.project.bluffball.domain.store.entity.StorePitchPurchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * 일일 상점 구종 구매 Repository.
 */
public interface StorePitchPurchaseRepository extends JpaRepository<StorePitchPurchase, Long> {

    /**
     * 당일 해당 구종 구매 여부.
     *
     * @param userId    유저 ID
     * @param offerDate 오퍼 날짜
     * @param cardId    구종 ID
     * @return 구매했으면 true
     */
    boolean existsByUserIdAndOfferDateAndCardId(Long userId, LocalDate offerDate, Long cardId);

    /**
     * 당일 구매한 구종 ID 목록.
     *
     * @param userId    유저 ID
     * @param offerDate 오퍼 날짜
     * @param cardIds   조회 대상 구종
     * @return 구매 기록
     */
    List<StorePitchPurchase> findByUserIdAndOfferDateAndCardIdIn(
            Long userId, LocalDate offerDate, Collection<Long> cardIds);
}
