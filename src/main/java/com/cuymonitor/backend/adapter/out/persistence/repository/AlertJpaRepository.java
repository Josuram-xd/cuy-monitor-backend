package com.cuymonitor.backend.adapter.out.persistence.repository;

import com.cuymonitor.backend.adapter.out.persistence.entity.AlertJpaEntity;
import com.cuymonitor.backend.domain.model.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertJpaRepository extends JpaRepository<AlertJpaEntity, Long> {
    List<AlertJpaEntity> findAllByOrderByCreatedAtDesc();
    List<AlertJpaEntity> findByStatusOrderByCreatedAtDesc(AlertStatus status);
}
