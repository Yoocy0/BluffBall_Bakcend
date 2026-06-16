package com.project.bluffball.domain.card.repository;

import com.project.bluffball.domain.card.entity.PitchCard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PitchCardRepository extends JpaRepository<PitchCard, Long> {
}
