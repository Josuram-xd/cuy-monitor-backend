package com.cuymonitor.backend.adapter.out.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otp_challenge")
public class OtpChallengeJpaEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected OtpChallengeJpaEntity() {}

    public OtpChallengeJpaEntity(UUID id, UUID userId, String codeHash, Instant createdAt, Instant expiresAt,
                                 int maxAttempts, int attempts, Instant usedAt, Instant revokedAt) {
        this.id = id;
        this.userId = userId;
        this.codeHash = codeHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.maxAttempts = maxAttempts;
        this.attempts = attempts;
        this.usedAt = usedAt;
        this.revokedAt = revokedAt;
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

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
