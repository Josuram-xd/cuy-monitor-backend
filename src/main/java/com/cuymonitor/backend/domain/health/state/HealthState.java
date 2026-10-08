package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.HealthStatus;

// each state answers where the guinea pig goes next; returning itself means "stay"
public interface HealthState {

    HealthStatus status();

    HealthState onSustainedAnomaly();

    HealthState onSustainedRecovery();
}
