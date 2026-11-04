package com.cuymonitor.backend.domain.port.out;

import java.util.List;
import java.time.Instant;
import com.cuymonitor.backend.domain.model.WeightReading;

import java.util.Optional;

public interface WeightReadingRepository {
    WeightReading save(WeightReading reading);
    Optional<WeightReading> findLatest(String cageCode);

    /** Every reading of a cage (stable or not) with from <= measuredAt <= to, oldest first. */
    List<WeightReading> findBetween(String cageCode, Instant from, Instant to);
}
