package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.adapter.out.persistence.mapper.RefreshTokenPersistenceMapper;
import com.cuymonitor.backend.adapter.out.persistence.repository.RefreshTokenJpaRepository;
import com.cuymonitor.backend.domain.model.auth.RefreshToken;
import com.cuymonitor.backend.domain.port.out.RefreshTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class RefreshTokenPersistenceAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository refreshTokenJpaRepository;
    private final RefreshTokenPersistenceMapper mapper;

    public RefreshTokenPersistenceAdapter(RefreshTokenJpaRepository refreshTokenJpaRepository,
                                          RefreshTokenPersistenceMapper mapper) {
        this.refreshTokenJpaRepository = refreshTokenJpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public RefreshToken save(RefreshToken token) {
        return mapper.toDomain(refreshTokenJpaRepository.save(mapper.toEntity(token)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return refreshTokenJpaRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void revokeFamily(UUID familyId, Instant now) {
        refreshTokenJpaRepository.revokeFamily(familyId, now);
    }

    @Override
    @Transactional
    public void deleteExpired(Instant now) {
        refreshTokenJpaRepository.deleteByExpiresAtBefore(now);
    }
}
