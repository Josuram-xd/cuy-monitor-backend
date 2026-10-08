package com.cuymonitor.backend.domain.model;

// what the producer measured; one implementation per EventType
public sealed interface HealthSignal permits BehaviorSignal, AudioSignal, WeightSignal {

    EventType type();
}
