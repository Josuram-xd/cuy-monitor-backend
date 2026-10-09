package com.cuymonitor.backend.adapter.out.persistence.repository;

import com.cuymonitor.backend.adapter.out.persistence.entity.CageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CageJpaRepository extends JpaRepository<CageJpaEntity, Long> {
    Optional<CageJpaEntity> findByCode(String code);
}
