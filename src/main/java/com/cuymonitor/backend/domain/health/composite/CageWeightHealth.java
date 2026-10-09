package com.cuymonitor.backend.domain.health.composite;

import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.WeightReading;

import java.util.Optional;

public class CageWeightHealth implements HealthComponent {

    private final WeightReading lastReading;

    public CageWeightHealth(Optional<WeightReading> lastReading) {
        this.lastReading = lastReading.orElse(null);
    }

    // the weight trend comes in Task 12.2; until then the scale never raises the cage status
    @Override
    public HealthStatus status() {
        return HealthStatus.NORMAL;
    }

    public Optional<WeightReading> lastReading() {
        return Optional.ofNullable(lastReading);
    }
}
