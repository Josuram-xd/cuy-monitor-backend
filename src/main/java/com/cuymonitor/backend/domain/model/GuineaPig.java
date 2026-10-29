package com.cuymonitor.backend.domain.model;

import java.time.Instant;
import java.util.Objects;

public class GuineaPig {

    private final Long id;
    private final String cageCode;
    private final String name;
    private final String markColor;
    private HealthStatus status;
    private Instant statusSince;
    private boolean active;
    private final Instant createdAt;
    private final GuineaPigProfile profile;

    private GuineaPig(Long id, String cageCode, String name, String markColor, HealthStatus status,
                      Instant statusSince, boolean active, Instant createdAt, GuineaPigProfile profile) {
        this.id = id;
        this.cageCode = Objects.requireNonNull(cageCode, "cageCode");
        this.name = requireName(name);
        this.markColor = requireMarkColor(markColor);
        this.status = Objects.requireNonNull(status, "status");
        this.statusSince = Objects.requireNonNull(statusSince, "statusSince");
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.profile = profile == null ? GuineaPigProfile.EMPTY : profile;
    }

    public static GuineaPig register(String cageCode, String name, String markColor, Instant now) {
        return register(cageCode, name, markColor, GuineaPigProfile.EMPTY, now);
    }

    public static GuineaPig register(String cageCode, String name, String markColor, GuineaPigProfile profile,
                                     Instant now) {
        return new GuineaPig(null, cageCode, name, markColor, HealthStatus.NORMAL, now, true, now, profile);
    }

    public static GuineaPig restore(Long id, String cageCode, String name, String markColor, HealthStatus status,
                                    Instant statusSince, boolean active, Instant createdAt) {
        return restore(id, cageCode, name, markColor, status, statusSince, active, createdAt, GuineaPigProfile.EMPTY);
    }

    public static GuineaPig restore(Long id, String cageCode, String name, String markColor, HealthStatus status,
                                    Instant statusSince, boolean active, Instant createdAt, GuineaPigProfile profile) {
        return new GuineaPig(id, cageCode, name, markColor, status, statusSince, active, createdAt, profile);
    }

    /** Soft delete: it keeps its history but is no longer an active guinea pig of the cage. */
    public void deactivate() {
        active = false;
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

    public String getMarkColor() {
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

    public GuineaPigProfile getProfile() {
        return profile;
    }

    // a MARK_COLOR code of catalog_option; that the row exists is checked by GuineaPigService
    private static String requireMarkColor(String markColor) {
        String code = CatalogOption.normalize(markColor);
        if (code == null) {
            throw new IllegalArgumentException("markColor is required");
        }
        if (code.length() > CatalogOption.MAX_CODE_LENGTH) {
            throw new IllegalArgumentException(
                    "markColor must have at most " + CatalogOption.MAX_CODE_LENGTH + " characters");
        }
        return code;
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
