package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.HealthStatus;

public interface HealthState {

    HealthStatus status();
}
