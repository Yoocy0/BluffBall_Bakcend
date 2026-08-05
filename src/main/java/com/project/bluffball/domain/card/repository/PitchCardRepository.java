package com.project.bluffball.domain.card.repository;

import com.project.bluffball.domain.card.entity.PitchCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PitchCardRepository extends JpaRepository<PitchCard, Long> {

    boolean existsByName(String name);

    Optional<PitchCard> findByName(String name);

    List<PitchCard> findByNameIn(Collection<String> names);

    @Query("SELECT p.id FROM PitchCard p")
    List<Long> findAllIds();

    @Query("SELECT p FROM PitchCard p WHERE p.id IN :ids")
    List<PitchCard> findAllByIds(@Param("ids") List<Long> ids);
}
