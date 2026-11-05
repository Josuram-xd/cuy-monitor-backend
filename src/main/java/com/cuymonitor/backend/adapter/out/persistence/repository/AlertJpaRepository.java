package com.cuymonitor.backend.adapter.out.persistence.repository;

import com.cuymonitor.backend.adapter.out.persistence.entity.AlertJpaEntity;
import com.cuymonitor.backend.domain.model.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AlertJpaRepository extends JpaRepository<AlertJpaEntity, Long>,
        JpaSpecificationExecutor<AlertJpaEntity> {
    @EntityGraph(attributePaths = {"cage", "guineaPig"})
    List<AlertJpaEntity> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"cage", "guineaPig"})
    List<AlertJpaEntity> findByStatusOrderByCreatedAtDesc(AlertStatus status);

    // the history: the cage and the guinea pig come in the same query, not one query per alert
    @Override
    @EntityGraph(attributePaths = {"cage", "guineaPig"})
    Page<AlertJpaEntity> findAll(Specification<AlertJpaEntity> specification, Pageable pageable);
}
