package com.cuymonitor.backend.adapter.out.persistence.repository;

import com.cuymonitor.backend.adapter.out.persistence.entity.CageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CageJpaRepository extends JpaRepository<CageJpaEntity, Long> {
    // any cage, active or not: events and alerts of a deleted cage still point to it
    Optional<CageJpaEntity> findByCode(String code);

    Optional<CageJpaEntity> findByCodeAndActiveTrue(String code);

    List<CageJpaEntity> findByOwnerIdAndActiveTrueOrderByCreatedAtAscIdAsc(UUID ownerId);

    boolean existsByCode(String code);
}
