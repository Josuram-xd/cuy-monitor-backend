package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.HealthEvent;

public interface RecordEventUseCase {
    /**
     * Stores the event once. False when there is nothing new to evaluate: a repeated eventId (producers
     * retry) or a behavior window of a mark color nobody wears in that cage.
     *
     * @throws IllegalArgumentException if the cage does not exist
     */
    boolean record(HealthEvent event);
}
