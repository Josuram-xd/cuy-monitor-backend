package com.cuymonitor.backend.domain.model;

import java.util.Objects;

// one 60 s window of one guinea pig, identified by the color of its mark
public record BehaviorSignal(MarkColor color, int windowSeconds, double stillSeconds, int feederVisits,
                             int watererVisits, double avgGroupDistance, double probAnomaly,
                             double detectionConfidence) implements HealthSignal {

    public BehaviorSignal {
        Objects.requireNonNull(color, "color");
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("windowSeconds must be positive");
        }
        if (stillSeconds < 0 || stillSeconds > windowSeconds) {
            throw new IllegalArgumentException("stillSeconds must be between 0 and windowSeconds");
        }
        if (feederVisits < 0 || watererVisits < 0) {
            throw new IllegalArgumentException("visits cannot be negative");
        }
        requireRatio(avgGroupDistance, "avgGroupDistance");
        requireRatio(probAnomaly, "probAnomaly");
        requireRatio(detectionConfidence, "detectionConfidence");
    }

    @Override
    public EventType type() {
        return EventType.BEHAVIOR;
    }

    static void requireRatio(double value, String name) {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException(name + " must be between 0 and 1");
        }
    }
}
