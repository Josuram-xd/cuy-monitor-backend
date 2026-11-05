package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.HealthStatus;

import java.time.Duration;

/**
 * <b>State pattern — concrete state</b> OBSERVED: goes to ALERT when the anomaly is still confirmed after
 * observedToAlert.
 */
public final class ObservedState implements HealthState {

    @Override
    public HealthStatus status() {
        return HealthStatus.OBSERVED;
    }

    @Override
    public HealthState onAnomaly(Duration timeInState, EscalationPolicy policy) {
        // the anomaly has to last observedToAlert at this level before it goes up
        return timeInState.compareTo(policy.observedToAlert()) >= 0 ? new AlertState() : this;
    }
}
