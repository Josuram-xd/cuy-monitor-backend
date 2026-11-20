package com.cuymonitor.backend.domain.exception;

/** The AI service already runs as many video simulations as it allows. Answered with 409. */
public class SimulationBusyException extends RuntimeException {

    public SimulationBusyException() {
        super("other simulations are running, try again in a few minutes");
    }
}
