package com.cuymonitor.backend.domain.port.out;

import com.cuymonitor.backend.domain.model.WeightReading;

import java.util.Optional;

public interface WeightReadingRepository {
    WeightReading save(WeightReading reading);
    Optional<WeightReading> findLatest(String cageCode);
}
