package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.HealthStatus;

/** <b>State pattern — concrete state</b> OBSERVED: one more confirmed anomaly moves it to ALERT. */
public final class ObservedState implements HealthState {

    @Override
    public HealthStatus status() {
        return HealthStatus.OBSERVED;
    }

    @Override
    public HealthState onAnomaly() {
        return new AlertState();
    }
}
