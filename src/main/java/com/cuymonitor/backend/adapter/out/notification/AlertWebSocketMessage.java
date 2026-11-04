package com.cuymonitor.backend.adapter.out.notification;

import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthStatus;
import java.time.Instant;

public record AlertWebSocketMessage(String type, String cageId, Instant occurredAt, Data data) {

    public record Data(Long id, String cageId, Long guineaPigId, HealthStatus level, EventType type,
                       String message, AlertStatus status, Instant createdAt, Instant reviewedAt) {
    }
}
