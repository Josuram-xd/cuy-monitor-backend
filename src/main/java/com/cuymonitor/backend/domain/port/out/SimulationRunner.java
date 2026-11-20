package com.cuymonitor.backend.domain.port.out;

import java.util.List;

/** Whoever draws and analyses the demo cage (the AI service). The first mark color is the sick guinea pig. */
public interface SimulationRunner {

    void start(String cageCode, List<String> markColors);

    /** The mark colors of the video clip, for the guinea pigs of the simulation cage. */
    List<String> clipColors();

    /** Stops the run of this cage. Nothing happens if it is not running. */
    void stop(String cageCode);

    /** The AI service's own answer about the run (running, framesSent, credit...), passed on as it is. */
    String status(String cageCode);
}
