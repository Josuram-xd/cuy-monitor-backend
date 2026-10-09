package com.cuymonitor.backend.adapter.out.persistence.repository;

import com.cuymonitor.backend.adapter.out.persistence.entity.BaselineProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BaselineProfileJpaRepository extends JpaRepository<BaselineProfileJpaEntity, Long> {
    Optional<BaselineProfileJpaEntity> findByGuineaPig_Id(long guineaPigId);
}
