package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthStatus;

import java.time.Instant;

/** Shape of an alert in docs/contracts/rest-api.md, 3.5. guineaPigId and reviewedAt can be null. */
public record AlertResponse(long id, String cageId, Long guineaPigId, HealthStatus level, EventType type,
                            String message, AlertStatus status, Instant createdAt, Instant reviewedAt) {

    public static AlertResponse from(Alert alert) {
        return new AlertResponse(alert.getId(), alert.getCageCode(), alert.getGuineaPigId(), alert.getLevel(),
                alert.getType(), alert.getMessage(), alert.getStatus(), alert.getCreatedAt(), alert.getReviewedAt());
    }
}
