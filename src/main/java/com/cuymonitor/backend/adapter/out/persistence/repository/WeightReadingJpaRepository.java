package com.cuymonitor.backend.adapter.out.persistence.repository;

import com.cuymonitor.backend.adapter.out.persistence.entity.WeightReadingJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WeightReadingJpaRepository extends JpaRepository<WeightReadingJpaEntity, Long> {
    @EntityGraph(attributePaths = "cage")
    Optional<WeightReadingJpaEntity> findFirstByCage_CodeOrderByMeasuredAtDesc(String cageCode);
}
