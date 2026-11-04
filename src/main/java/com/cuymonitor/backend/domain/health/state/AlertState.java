package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.HealthStatus;

import java.time.Duration;

/**
 * <b>State pattern — concrete state</b> ALERT: goes to CRITICAL when the anomaly is still confirmed after
 * alertToCritical.
 */
public final class AlertState implements HealthState {

    @Override
    public HealthStatus status() {
        return HealthStatus.ALERT;
    }

    @Override
    public HealthState onAnomaly(Duration timeInState, EscalationPolicy policy) {
        // the anomaly has to last alertToCritical at this level before it goes up
        return timeInState.compareTo(policy.alertToCritical()) >= 0 ? new CriticalState() : this;
    }

    @Override
    public boolean raisesAlert() {
        return true;
    }
}
