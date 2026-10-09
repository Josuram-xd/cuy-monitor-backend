package com.cuymonitor.backend.domain.model.auth;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A stored refresh token. Only the hash of the random value is kept. All the tokens that come from one
 * login share a family, so a stolen token that gets reused can take the whole family down.
 */
public class RefreshToken {

    private final UUID id;
    private final UUID userId;
    private final UUID familyId;
    private final String tokenHash;
    private final Instant createdAt;
    private final Instant expiresAt;
    private Instant revokedAt;

    private RefreshToken(UUID id, UUID userId, UUID familyId, String tokenHash, Instant createdAt, Instant expiresAt,
                         Instant revokedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.familyId = Objects.requireNonNull(familyId, "familyId");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.revokedAt = revokedAt;
    }

    public static RefreshToken issue(UUID userId, UUID familyId, String tokenHash, Instant now, Instant expiresAt) {
        return new RefreshToken(UUID.randomUUID(), userId, familyId, tokenHash, now, expiresAt, null);
    }

    public static RefreshToken restore(UUID id, UUID userId, UUID familyId, String tokenHash, Instant createdAt,
                                       Instant expiresAt, Instant revokedAt) {
        return new RefreshToken(id, userId, familyId, tokenHash, createdAt, expiresAt, revokedAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    /** Revoking twice keeps the first date. */
    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Optional<Instant> getRevokedAt() {
        return Optional.ofNullable(revokedAt);
    }
}
