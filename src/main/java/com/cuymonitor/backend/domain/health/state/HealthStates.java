package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.HealthStatus;

/** Rebuilds the state object of a guinea pig from the status stored in the database. */
public final class HealthStates {

    private HealthStates() {
    }

    public static HealthState of(HealthStatus status) {
        return switch (status) {
            case NORMAL -> new NormalState();
            case OBSERVED -> new ObservedState();
            case ALERT -> new AlertState();
            case CRITICAL -> new CriticalState();
        };
    }
}
