package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.HealthStatus;

import java.time.Duration;

/** <b>State pattern — concrete state</b> CRITICAL: the worst level: another anomaly keeps it here. */
public final class CriticalState implements HealthState {

    @Override
    public HealthStatus status() {
        return HealthStatus.CRITICAL;
    }

    @Override
    public HealthState onAnomaly(Duration timeInState, EscalationPolicy policy) {
        return this;
    }

    @Override
    public boolean raisesAlert() {
        return true;
    }
}
