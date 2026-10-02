package com.cuymonitor.backend.domain.model.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class OtpChallenge {

    private final UUID id;
    private final UUID userId;
    private final String codeHash;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final int maxAttempts;
    private int attempts;
    private Instant usedAt;
    private Instant revokedAt;

    private OtpChallenge(UUID id, UUID userId, String codeHash, Instant createdAt, Instant expiresAt,
                         int maxAttempts, int attempts, Instant usedAt, Instant revokedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.codeHash = Objects.requireNonNull(codeHash, "codeHash");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        if (attempts < 0) {
            throw new IllegalArgumentException("attempts cannot be negative");
        }
        this.maxAttempts = maxAttempts;
        this.attempts = attempts;
        this.usedAt = usedAt;
        this.revokedAt = revokedAt;
    }
    public static OtpChallenge issue(UUID userId, String codeHash, Instant now, Duration ttl, int maxAttempts) {
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(ttl, "ttl");
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        return new OtpChallenge(UUID.randomUUID(), userId, codeHash, now, now.plus(ttl),
                maxAttempts, 0, null, null);
    }

    public static OtpChallenge restore(UUID id, UUID userId, String codeHash, Instant createdAt, Instant expiresAt,
                                       int maxAttempts, int attempts, Instant usedAt, Instant revokedAt) {
        return new OtpChallenge(id, userId, codeHash, createdAt, expiresAt, maxAttempts, attempts, usedAt, revokedAt);
    }

    public OtpVerificationResult verify(boolean codeMatches, Instant now) {
        Objects.requireNonNull(now, "now");
        if (usedAt != null || revokedAt != null) {
            return OtpVerificationResult.ALREADY_USED;
        }
        if (isExpired(now)) {
            return OtpVerificationResult.EXPIRED;
        }
        if (attempts >= maxAttempts) {
            return OtpVerificationResult.TOO_MANY_ATTEMPTS;
        }
        if (!codeMatches) {
            attempts++;
            return OtpVerificationResult.WRONG_CODE;
        }
        usedAt = now;
        return OtpVerificationResult.VERIFIED;
    }

    public void revoke(Instant now) {
        Objects.requireNonNull(now, "now");
        if (usedAt == null && revokedAt == null) {
            revokedAt = now;
        }
    }

    private boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public int getAttempts() {
        return attempts;
    }

    public Optional<Instant> getUsedAt() {
        return Optional.ofNullable(usedAt);
    }

    public Optional<Instant> getRevokedAt() {
        return Optional.ofNullable(revokedAt);
    }
}
