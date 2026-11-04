package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.HealthStatus;

/** <b>State pattern — concrete state</b> NORMAL: one more confirmed anomaly moves it to OBSERVED. */
public final class NormalState implements HealthState {

    @Override
    public HealthStatus status() {
        return HealthStatus.NORMAL;
    }

    @Override
    public HealthState onAnomaly() {
        return new ObservedState();
    }
}
