package com.cuymonitor.backend.application.fake;

import com.cuymonitor.backend.domain.port.out.RevokedTokenRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InMemoryRevokedTokenRepository implements RevokedTokenRepository {

    private final Map<UUID, Instant> revoked = new HashMap<>();

    @Override
    public void revoke(UUID id, Instant expiresAt) {
        revoked.put(id, expiresAt);
    }

    @Override
    public boolean isRevoked(UUID id) {
        return revoked.containsKey(id);
    }

    @Override
    public void deleteExpired(Instant now) {
        revoked.values().removeIf(expiresAt -> expiresAt.isBefore(now));
    }
}
