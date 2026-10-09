package com.cuymonitor.backend.domain.model;

// declared from best to worst: the order is what "worse" means
public enum HealthStatus {
    NORMAL,
    OBSERVED,
    ALERT,
    CRITICAL;

    public boolean isWorseThan(HealthStatus other) {
        return compareTo(other) > 0;
    }

    public boolean raisesAlert() {
        return this == ALERT || this == CRITICAL;
    }

    public static HealthStatus worst(HealthStatus a, HealthStatus b) {
        return a.isWorseThan(b) ? a : b;
    }
}
