package com.project.bluffball.domain.card.repository;

import com.project.bluffball.domain.card.entity.CoordinateCard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoordinateCardRepository extends JpaRepository<CoordinateCard, Long> {
}
