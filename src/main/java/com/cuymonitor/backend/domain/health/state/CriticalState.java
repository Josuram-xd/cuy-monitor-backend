package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.HealthStatus;

public class CriticalState implements HealthState {

    @Override
    public HealthStatus status() {
        return HealthStatus.CRITICAL;
    }

    // already the worst level: more anomalies keep it here
    @Override
    public HealthState onSustainedAnomaly() {
        return this;
    }

    @Override
    public HealthState onSustainedRecovery() {
        return new NormalState();
    }
}
