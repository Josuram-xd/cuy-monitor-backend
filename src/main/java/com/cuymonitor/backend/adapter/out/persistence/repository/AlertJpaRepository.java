package com.cuymonitor.backend.adapter.out.persistence.repository;

import com.cuymonitor.backend.adapter.out.persistence.entity.AlertJpaEntity;
import com.cuymonitor.backend.domain.model.AlertStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertJpaRepository extends JpaRepository<AlertJpaEntity, Long> {
    @EntityGraph(attributePaths = {"cage", "guineaPig"})
    List<AlertJpaEntity> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"cage", "guineaPig"})
    List<AlertJpaEntity> findByStatusOrderByCreatedAtDesc(AlertStatus status);
}
