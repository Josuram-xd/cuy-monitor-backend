package com.cuymonitor.backend.domain.port.out;

import java.util.List;

/** Whoever draws and analyses the demo cage (the AI service). The first mark color is the sick guinea pig. */
public interface SimulationRunner {

    void start(String cageCode, List<String> markColors);
}
