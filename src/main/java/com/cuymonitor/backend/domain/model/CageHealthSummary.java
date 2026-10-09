package com.cuymonitor.backend.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** What the dashboard shows for a cage: its overall status and the parts it comes from. */
public record CageHealthSummary(String cageCode, HealthStatus status, List<GuineaPig> guineaPigs, Audio audio,
                                Weight weight, Instant updatedAt) {

    public CageHealthSummary {
        Objects.requireNonNull(cageCode, "cageCode");
        Objects.requireNonNull(status, "status");
        guineaPigs = List.copyOf(guineaPigs);
        Objects.requireNonNull(audio, "audio");
        Objects.requireNonNull(weight, "weight");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /** lastEventAt is null when no audio event arrived yet. */
    public record Audio(HealthStatus status, Instant lastEventAt) {
    }

    /** lastGrams and lastMeasuredAt are null when nothing was weighed yet. */
    public record Weight(HealthStatus status, Double lastGrams, Instant lastMeasuredAt) {
    }
}
