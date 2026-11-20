package com.cuymonitor.backend.domain.model;

import java.util.Objects;

public record AudioSignal(AudioLabel label, double probability, int durationMs) implements HealthSignal {

    public AudioSignal {
        Objects.requireNonNull(label, "label");
        BehaviorSignal.requireRatio(probability, "probability");
        if (durationMs <= 0) {
            throw new IllegalArgumentException("durationMs must be positive");
        }
    }

    @Override
    public EventType type() {
        return EventType.AUDIO;
    }

    @Override
    public <R> R accept(HealthSignalVisitor<R> visitor) {
        return visitor.visitAudio(this);
    }
}
