package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.HealthStatus;

public class ObservedState implements HealthState {

    @Override
    public HealthStatus status() {
        return HealthStatus.OBSERVED;
    }

    @Override
    public HealthState onSustainedAnomaly() {
        return new AlertState();
    }

    @Override
    public HealthState onSustainedRecovery() {
        return new NormalState();
    }
}
