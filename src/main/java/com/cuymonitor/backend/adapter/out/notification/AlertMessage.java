package com.cuymonitor.backend.adapter.out.notification;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthStatus;

import java.time.Instant;

public record AlertMessage(Long id, String cageId, Long guineaPigId, HealthStatus level, EventType type,
                           String message, AlertStatus status, Instant createdAt, Instant reviewedAt) {

    public static AlertMessage from(Alert alert) {
        return new AlertMessage(alert.getId(), alert.getCageCode(), alert.getGuineaPigId(), alert.getLevel(),
                alert.getType(), alert.getMessage(), alert.getStatus(), alert.getCreatedAt(), alert.getReviewedAt());
    }
}
