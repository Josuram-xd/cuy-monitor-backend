package com.cuymonitor.backend.domain.health.composite;

import com.cuymonitor.backend.domain.model.HealthStatus;
import java.util.Objects;

public final class CageWeightHealth implements HealthComponent {

    private final HealthStatus status;

    public CageWeightHealth(HealthStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    @Override
    public HealthStatus status() {
        return status;
    }
}
