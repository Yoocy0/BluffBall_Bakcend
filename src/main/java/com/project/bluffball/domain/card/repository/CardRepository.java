package com.project.bluffball.domain.card.repository;

import com.project.bluffball.domain.card.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {
}
