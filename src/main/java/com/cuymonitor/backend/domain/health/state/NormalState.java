package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.HealthStatus;

public class NormalState implements HealthState {

    @Override
    public HealthStatus status() {
        return HealthStatus.NORMAL;
    }

    @Override
    public HealthState onSustainedAnomaly() {
        return new ObservedState();
    }

    @Override
    public HealthState onSustainedRecovery() {
        return this;
    }
}
