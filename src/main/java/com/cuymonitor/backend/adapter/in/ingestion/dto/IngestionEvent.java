package com.cuymonitor.backend.adapter.in.ingestion.dto;

import com.cuymonitor.backend.domain.model.EventType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
        @Min(1) @Max(1) int schemaVersion,
        @NotNull Map<String, Object> payload) {
}
