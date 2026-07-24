package com.project.bluffball.domain.league.repository;

import com.project.bluffball.domain.league.entity.LeaguePrizeRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 리그 상금 비율 규칙 JPA Repository.
 */
public interface LeaguePrizeRuleRepository extends JpaRepository<LeaguePrizeRule, Long> {

    /**
     * 순위 오름차순으로 상금 규칙을 조회한다.
     *
     * @return 상금 규칙 목록
     */
    List<LeaguePrizeRule> findAllByOrderByRankPositionAsc();
}
