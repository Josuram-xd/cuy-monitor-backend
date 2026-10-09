package com.cuymonitor.backend.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

// revoked_at is not mapped on purpose: the column defaults to now() in the database
@Entity
@Table(name = "revoked_token")
public class RevokedTokenJpaEntity {
    @Id
    private UUID jti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected RevokedTokenJpaEntity() {}

    public RevokedTokenJpaEntity(UUID jti, Instant expiresAt) {
        this.jti = jti;
        this.expiresAt = expiresAt;
    }

    public UUID getJti() {
        return jti;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
