package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.HealthStatus;

public final class AlertState implements HealthState {

    @Override
    public HealthStatus status() {
        return HealthStatus.ALERT;
    }
}
