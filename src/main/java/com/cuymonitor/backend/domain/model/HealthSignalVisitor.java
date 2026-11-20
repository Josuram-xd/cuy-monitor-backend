package com.cuymonitor.backend.domain.model;

/**
 * <b>Visitor.</b> One operation over the three signal types; a new signal type breaks every visitor at compile time.
 */
public interface HealthSignalVisitor<R> {

    R visitBehavior(BehaviorSignal signal);

    R visitAudio(AudioSignal signal);

    R visitWeight(WeightSignal signal);
}
