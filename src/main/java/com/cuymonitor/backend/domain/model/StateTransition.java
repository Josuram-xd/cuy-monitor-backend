package com.cuymonitor.backend.domain.model;

import java.time.Instant;
import java.util.Objects;

public record StateTransition(Long id, long guineaPigId, HealthStatus fromStatus, HealthStatus toStatus,
                              String reason, Instant occurredAt) {

    public StateTransition {
        Objects.requireNonNull(fromStatus, "fromStatus");
        Objects.requireNonNull(toStatus, "toStatus");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
