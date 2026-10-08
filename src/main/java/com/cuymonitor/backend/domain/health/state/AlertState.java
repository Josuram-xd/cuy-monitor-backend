package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.HealthStatus;

public class AlertState implements HealthState {

    @Override
    public HealthStatus status() {
        return HealthStatus.ALERT;
    }

    @Override
    public HealthState onSustainedAnomaly() {
        return new CriticalState();
    }

    @Override
    public HealthState onSustainedRecovery() {
        return new NormalState();
    }
}
