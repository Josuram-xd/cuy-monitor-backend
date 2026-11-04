package com.cuymonitor.backend.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cage")
public class CageJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected CageJpaEntity() {
    }

    public CageJpaEntity(Long id, String code, String name, String location) {
        this(id, code, name, location, null, true, null);
    }

    public CageJpaEntity(Long id, String code, String name, String location, UUID ownerId, boolean active,
                         Instant createdAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.location = location;
        this.ownerId = ownerId;
        this.active = active;
        this.createdAt = createdAt;
    }

    // a cage built without a date (the older constructor) is stamped when it is first stored
    @PrePersist
    void stampCreation() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public boolean isActive() {
        return active;
    }
}
