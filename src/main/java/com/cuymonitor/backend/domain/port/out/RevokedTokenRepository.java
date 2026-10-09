package com.cuymonitor.backend.domain.port.out;

import java.time.Instant;
import java.util.UUID;

public interface RevokedTokenRepository {
    void revoke(UUID id, Instant expiresAt);
    boolean isRevoked(UUID id);
    void deleteExpired(Instant now);
}
