package com.cuymonitor.backend.ingestion.dto;

import com.cuymonitor.backend.domain.model.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Common envelope sent by ai-service (BEHAVIOR, AUDIO) and the serial bridge (WEIGHT).
 * The payload shape depends on the type; the adapters convert it to HealthEvent.
 */
public record IngestionEvent(
        @NotNull UUID eventId,
        @NotNull EventType type,
        @NotBlank String cageId,
        @NotNull Instant timestamp,
        @NotBlank String source,
        int schemaVersion,
        @NotNull Map<String, Object> payload) {
}
