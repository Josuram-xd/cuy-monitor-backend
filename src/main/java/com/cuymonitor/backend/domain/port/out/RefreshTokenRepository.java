package com.cuymonitor.backend.domain.port.out;

import com.cuymonitor.backend.domain.model.auth.RefreshToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken token);
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void revokeFamily(UUID familyId, Instant now);
    void deleteExpired(Instant now);
}
