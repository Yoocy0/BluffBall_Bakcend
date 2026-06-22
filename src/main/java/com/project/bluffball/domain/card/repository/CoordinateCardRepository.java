package com.project.bluffball.domain.card.repository;

import com.project.bluffball.domain.card.entity.CoordinateCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoordinateCardRepository extends JpaRepository<CoordinateCard, Long> {

    Optional<CoordinateCard> findByCoordinateNumber(int coordinateNumber);
}
