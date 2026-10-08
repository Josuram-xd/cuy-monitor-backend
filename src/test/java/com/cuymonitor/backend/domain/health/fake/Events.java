package com.cuymonitor.backend.domain.health.fake;

import com.cuymonitor.backend.domain.model.AudioLabel;
import com.cuymonitor.backend.domain.model.AudioSignal;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.MarkColor;
import com.cuymonitor.backend.domain.model.WeightSignal;

import java.time.Instant;
import java.util.UUID;

// builders for the events used in the tests
public final class Events {

    private Events() {
    }

    public static HealthEvent normalWindow(MarkColor color, Instant at) {
        return behavior(color, at, 10, 2, 0.2, 0.1, 0.9);
    }

    // still most of the minute and never went to the feeder
    public static HealthEvent anomalousWindow(MarkColor color, Instant at) {
        return behavior(color, at, 55, 0, 0.2, 0.1, 0.9);
    }

    public static HealthEvent behavior(MarkColor color, Instant at, double still, int feeder, double groupDistance,
                                       double probAnomaly, double confidence) {
        return new HealthEvent(UUID.randomUUID(), "cage-1", at, "ai-service",
                new BehaviorSignal(color, 60, still, feeder, 1, groupDistance, probAnomaly, confidence));
    }

    public static HealthEvent audio(AudioLabel label, double probability, Instant at) {
        return new HealthEvent(UUID.randomUUID(), "cage-1", at, "ai-service", new AudioSignal(label, probability, 960));
    }

    public static HealthEvent weight(double grams, Instant at) {
        return new HealthEvent(UUID.randomUUID(), "cage-1", at, "arduino", new WeightSignal(grams, true));
    }
}
