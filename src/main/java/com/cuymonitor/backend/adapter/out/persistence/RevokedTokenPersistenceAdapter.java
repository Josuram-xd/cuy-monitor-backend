package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.adapter.out.persistence.entity.RevokedTokenJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.repository.RevokedTokenJpaRepository;
import com.cuymonitor.backend.domain.port.out.RevokedTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class RevokedTokenPersistenceAdapter implements RevokedTokenRepository {

    private final RevokedTokenJpaRepository revokedTokenJpaRepository;

    public RevokedTokenPersistenceAdapter(RevokedTokenJpaRepository revokedTokenJpaRepository) {
        this.revokedTokenJpaRepository = revokedTokenJpaRepository;
    }

    @Override
    @Transactional
    public void revoke(UUID id, Instant expiresAt) {
        revokedTokenJpaRepository.save(new RevokedTokenJpaEntity(id, expiresAt));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRevoked(UUID id) {
        return revokedTokenJpaRepository.existsById(id);
    }

    @Override
    @Transactional
    public void deleteExpired(Instant now) {
        revokedTokenJpaRepository.deleteByExpiresAtBefore(now);
    }
}
