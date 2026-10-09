package com.cuymonitor.backend.domain.model;

public record WeightSignal(double grams, boolean stable) implements HealthSignal {

    public WeightSignal {
        if (grams < 0) {
            throw new IllegalArgumentException("grams cannot be negative");
        }
    }

    @Override
    public EventType type() {
        return EventType.WEIGHT;
    }
}
