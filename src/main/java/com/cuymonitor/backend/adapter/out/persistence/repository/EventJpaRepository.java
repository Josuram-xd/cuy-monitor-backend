package com.cuymonitor.backend.adapter.out.persistence.repository;

import com.cuymonitor.backend.adapter.out.persistence.entity.EventJpaEntity;
import com.cuymonitor.backend.domain.model.EventType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventJpaRepository extends JpaRepository<EventJpaEntity, UUID> {
    @EntityGraph(attributePaths = {"cage", "guineaPig"})
    List<EventJpaEntity> findByGuineaPig_IdAndTypeOrderByOccurredAtDesc(long guineaPigId, EventType type,
                                                                         Pageable pageable);

    @EntityGraph(attributePaths = "cage")
    Optional<EventJpaEntity> findFirstByCage_CodeAndTypeOrderByOccurredAtDesc(String cageCode, EventType type);
}
