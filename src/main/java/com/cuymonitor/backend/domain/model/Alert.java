package com.cuymonitor.backend.domain.model;

import java.time.Instant;
import java.util.Objects;

public class Alert {

    private Long id;
    private final String cageCode;
    private final Long guineaPigId;
    private final HealthStatus level;
    private final EventType type;
    private final String message;
    private AlertStatus status;
    private final Instant createdAt;
    private Instant reviewedAt;

    private Alert(Long id, String cageCode, Long guineaPigId, HealthStatus level, EventType type, String message,
                  AlertStatus status, Instant createdAt, Instant reviewedAt) {
        this.id = id;
        this.cageCode = Objects.requireNonNull(cageCode, "cageCode");
        this.guineaPigId = guineaPigId;
        this.level = Objects.requireNonNull(level, "level");
        if (!level.raisesAlert()) {
            throw new IllegalArgumentException("an alert must be ALERT or CRITICAL");
        }
        this.type = Objects.requireNonNull(type, "type");
        this.message = Objects.requireNonNull(message, "message");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.reviewedAt = reviewedAt;
    }

    public static Alert forGuineaPig(GuineaPig guineaPig, HealthStatus level, String message, Instant now) {
        return new Alert(null, guineaPig.getCageCode(), guineaPig.getId(), level, EventType.BEHAVIOR, message,
                AlertStatus.OPEN, now, null);
    }

    // audio and weight alerts belong to the whole cage, so there is no guinea pig
    public static Alert forCage(String cageCode, EventType type, HealthStatus level, String message, Instant now) {
        return new Alert(null, cageCode, null, level, type, message, AlertStatus.OPEN, now, null);
    }

    public static Alert restore(Long id, String cageCode, Long guineaPigId, HealthStatus level, EventType type,
                                String message, AlertStatus status, Instant createdAt, Instant reviewedAt) {
        return new Alert(id, cageCode, guineaPigId, level, type, message, status, createdAt, reviewedAt);
    }

    // set once by the repository when the alert is stored
    public void assignId(long newId) {
        if (id != null && id != newId) {
            throw new IllegalStateException("alert already has an id");
        }
        id = newId;
    }

    public void markReviewed(Instant now) {
        if (status == AlertStatus.OPEN) {
            status = AlertStatus.REVIEWED;
            reviewedAt = Objects.requireNonNull(now, "now");
        }
    }

    public Long getId() {
        return id;
    }

    public String getCageCode() {
        return cageCode;
    }

    public Long getGuineaPigId() {
        return guineaPigId;
    }

    public HealthStatus getLevel() {
        return level;
    }

    public EventType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }
}
