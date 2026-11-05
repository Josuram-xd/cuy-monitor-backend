package com.cuymonitor.backend.domain.model;

import java.util.List;
import java.util.Objects;

/** The scale readings of a cage in a time range, oldest first, stable or not (the client picks). */
public record CageWeight(String cageCode, TimeRange range, List<WeightReading> readings) {

    public CageWeight {
        Objects.requireNonNull(cageCode, "cageCode");
        Objects.requireNonNull(range, "range");
        readings = List.copyOf(readings);
    }
}
