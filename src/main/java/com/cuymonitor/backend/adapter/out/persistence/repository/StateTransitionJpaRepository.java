package com.cuymonitor.backend.adapter.out.persistence.repository;

import com.cuymonitor.backend.adapter.out.persistence.entity.StateTransitionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StateTransitionJpaRepository extends JpaRepository<StateTransitionJpaEntity, Long> {
}
