package com.cuymonitor.backend.adapter.out.persistence.repository;

import com.cuymonitor.backend.adapter.out.persistence.entity.OtpChallengeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OtpChallengeJpaRepository extends JpaRepository<OtpChallengeJpaEntity, UUID> {
    List<OtpChallengeJpaEntity> findByUserIdAndUsedAtIsNullAndRevokedAtIsNull(UUID userId);
}
