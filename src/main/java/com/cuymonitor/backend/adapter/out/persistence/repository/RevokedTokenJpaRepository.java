package com.cuymonitor.backend.adapter.out.persistence.repository;

import com.cuymonitor.backend.adapter.out.persistence.entity.RevokedTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface RevokedTokenJpaRepository extends JpaRepository<RevokedTokenJpaEntity, UUID> {
    void deleteByExpiresAtBefore(Instant now);
}
