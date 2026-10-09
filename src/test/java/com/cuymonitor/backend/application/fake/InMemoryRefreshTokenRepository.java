package com.cuymonitor.backend.application.fake;

import com.cuymonitor.backend.domain.model.auth.RefreshToken;
import com.cuymonitor.backend.domain.port.out.RefreshTokenRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryRefreshTokenRepository implements RefreshTokenRepository {

    private final Map<UUID, RefreshToken> tokens = new HashMap<>();

    @Override
    public RefreshToken save(RefreshToken token) {
        tokens.put(token.getId(), copy(token));
        return copy(token);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return tokens.values().stream()
                .filter(t -> t.getTokenHash().equals(tokenHash))
                .findFirst()
                .map(InMemoryRefreshTokenRepository::copy);
    }

    @Override
    public void revokeFamily(UUID familyId, Instant now) {
        tokens.values().stream().filter(t -> t.getFamilyId().equals(familyId)).forEach(t -> t.revoke(now));
    }

    @Override
    public void deleteExpired(Instant now) {
        tokens.values().removeIf(t -> t.getExpiresAt().isBefore(now));
    }

    public int count() {
        return tokens.size();
    }

    private static RefreshToken copy(RefreshToken t) {
        return RefreshToken.restore(t.getId(), t.getUserId(), t.getFamilyId(), t.getTokenHash(), t.getCreatedAt(),
                t.getExpiresAt(), t.getRevokedAt().orElse(null));
    }
}
