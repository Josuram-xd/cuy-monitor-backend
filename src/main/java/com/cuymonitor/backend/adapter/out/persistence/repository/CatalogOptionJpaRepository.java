package com.cuymonitor.backend.adapter.out.persistence.repository;

import com.cuymonitor.backend.adapter.out.persistence.entity.CatalogOptionJpaEntity;
import com.cuymonitor.backend.domain.model.CatalogKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CatalogOptionJpaRepository
        extends JpaRepository<CatalogOptionJpaEntity, CatalogOptionJpaEntity.Key> {

    List<CatalogOptionJpaEntity> findAllByActiveTrueOrderByKindAscSortOrderAsc();

    boolean existsByKindAndCodeAndActiveTrue(CatalogKind kind, String code);

    // the admin screen also shows the options that are switched off
    List<CatalogOptionJpaEntity> findAllByOrderByKindAscSortOrderAsc();

    Optional<CatalogOptionJpaEntity> findByKindAndCode(CatalogKind kind, String code);
}
