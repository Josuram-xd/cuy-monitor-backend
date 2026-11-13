package com.cuymonitor.backend.domain.exception;

/** The demo could not start: the cage has no guinea pigs to draw, or the AI service did not answer. */
public class SimulationUnavailableException extends RuntimeException {

    private final boolean noGuineaPigs;

    private SimulationUnavailableException(String message, boolean noGuineaPigs, Throwable cause) {
        super(message, cause);
        this.noGuineaPigs = noGuineaPigs;
    }

    public static SimulationUnavailableException noGuineaPigs() {
        return new SimulationUnavailableException("register at least one guinea pig to run the simulation", true,
                null);
    }

    public static SimulationUnavailableException aiServiceDown(Throwable cause) {
        return new SimulationUnavailableException("the AI service is not available right now", false, cause);
    }

    public boolean isNoGuineaPigs() {
        return noGuineaPigs;
    }
}
