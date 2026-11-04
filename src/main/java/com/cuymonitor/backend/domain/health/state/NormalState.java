package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.HealthStatus;

import java.time.Duration;

/** <b>State pattern — concrete state</b> NORMAL: one confirmed anomaly moves it to OBSERVED at once. */
public final class NormalState implements HealthState {

    @Override
    public HealthStatus status() {
        return HealthStatus.NORMAL;
    }

    @Override
    public HealthState onAnomaly(Duration timeInState, EscalationPolicy policy) {
        // the first confirmed anomaly is enough to start watching
        return new ObservedState();
    }
}
