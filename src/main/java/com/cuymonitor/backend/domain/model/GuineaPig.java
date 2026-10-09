package com.cuymonitor.backend.domain.model;

import java.time.Instant;
import java.util.Objects;

public class GuineaPig {

    private final Long id;
    private final String cageCode;
    private final String name;
    private final MarkColor markColor;
    private HealthStatus status;
    private Instant statusSince;
    private final boolean active;
    private final Instant createdAt;

    private GuineaPig(Long id, String cageCode, String name, MarkColor markColor, HealthStatus status,
                      Instant statusSince, boolean active, Instant createdAt) {
        this.id = id;
        this.cageCode = Objects.requireNonNull(cageCode, "cageCode");
        this.name = requireName(name);
        this.markColor = Objects.requireNonNull(markColor, "markColor");
        this.status = Objects.requireNonNull(status, "status");
        this.statusSince = Objects.requireNonNull(statusSince, "statusSince");
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static GuineaPig register(String cageCode, String name, MarkColor markColor, Instant now) {
        return new GuineaPig(null, cageCode, name, markColor, HealthStatus.NORMAL, now, true, now);
    }

    public static GuineaPig restore(Long id, String cageCode, String name, MarkColor markColor, HealthStatus status,
                                    Instant statusSince, boolean active, Instant createdAt) {
        return new GuineaPig(id, cageCode, name, markColor, status, statusSince, active, createdAt);
    }

    public void changeStatus(HealthStatus newStatus, Instant now) {
        Objects.requireNonNull(newStatus, "newStatus");
        if (newStatus != status) {
            status = newStatus;
            statusSince = Objects.requireNonNull(now, "now");
        }
    }

    public Long getId() {
        return id;
    }

    public String getCageCode() {
        return cageCode;
    }

    public String getName() {
        return name;
    }

    public MarkColor getMarkColor() {
        return markColor;
    }

    public HealthStatus getStatus() {
        return status;
    }

    public Instant getStatusSince() {
        return statusSince;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException("name must have at most 100 characters");
        }
        return trimmed;
    }
}
