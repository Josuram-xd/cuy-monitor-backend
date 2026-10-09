package com.cuymonitor.backend.domain.model;

import java.time.Instant;
import java.util.Objects;

public record WeightReading(Long id, String cageCode, double grams, boolean stable, Instant measuredAt) {

    public WeightReading {
        Objects.requireNonNull(cageCode, "cageCode");
        Objects.requireNonNull(measuredAt, "measuredAt");
        if (grams < 0) {
            throw new IllegalArgumentException("grams cannot be negative");
        }
    }
}
