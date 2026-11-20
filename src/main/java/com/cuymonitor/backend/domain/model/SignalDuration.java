package com.cuymonitor.backend.domain.model;

import java.time.Duration;

/**
 * <b>Visitor.</b> How far back a signal reaches from the moment it occurred: its window or its clip. A weight
 * reading is a point in time.
 */
public final class SignalDuration implements HealthSignalVisitor<Duration> {

    public static final SignalDuration INSTANCE = new SignalDuration();

    private SignalDuration() {
    }

    @Override
    public Duration visitBehavior(BehaviorSignal signal) {
        return Duration.ofSeconds(signal.windowSeconds());
    }

    @Override
    public Duration visitAudio(AudioSignal signal) {
        return Duration.ofMillis(signal.durationMs());
    }

    @Override
    public Duration visitWeight(WeightSignal signal) {
        return Duration.ZERO;
    }
}
