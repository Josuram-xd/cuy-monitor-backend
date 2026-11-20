package com.cuymonitor.backend.domain.port.in;

/**
 * How far a demo run got, and stopping it. The owner check is made by the caller before it gets here, as for
 * starting one.
 */
public interface ControlSimulationUseCase {

    void stop(String cageCode);

    /** The AI service's own JSON about the run: running, framesSent, frameCount, credit. */
    String status(String cageCode);
}
