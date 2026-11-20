package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.HealthStatus;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * <b>Flyweight — factory.</b> The four states hold no data of their own, so one shared instance each is enough.
 * Rebuilds the state object of a guinea pig from the status stored in the database.
 */
public final class HealthStates {

    private static final Map<HealthStatus, HealthState> SHARED = new EnumMap<>(Map.of(
            HealthStatus.NORMAL, new NormalState(),
            HealthStatus.OBSERVED, new ObservedState(),
            HealthStatus.ALERT, new AlertState(),
            HealthStatus.CRITICAL, new CriticalState()));

    private HealthStates() {
    }

    public static HealthState of(HealthStatus status) {
        return SHARED.get(Objects.requireNonNull(status, "status"));
    }
}
