package com.cuymonitor.backend.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// boundary between the data input (ingestion adapters) and the health core
public record HealthEvent(UUID eventId, String cageId, Instant occurredAt, String source, HealthSignal signal) {

    public HealthEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(cageId, "cageId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(signal, "signal");
    }

    public EventType type() {
        return signal.type();
    }
}
