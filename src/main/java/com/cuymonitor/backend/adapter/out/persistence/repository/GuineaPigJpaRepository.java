package com.cuymonitor.backend.adapter.out.persistence.repository;

import com.cuymonitor.backend.adapter.out.persistence.entity.GuineaPigJpaEntity;
import com.cuymonitor.backend.domain.model.MarkColor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GuineaPigJpaRepository extends JpaRepository<GuineaPigJpaEntity, Long> {
    @EntityGraph(attributePaths = "cage")
    Optional<GuineaPigJpaEntity> findByCage_CodeAndMarkColorAndActiveTrue(String cageCode, MarkColor markColor);

    @EntityGraph(attributePaths = "cage")
    List<GuineaPigJpaEntity> findAllByCage_CodeAndActiveTrueOrderByIdAsc(String cageCode);
}
