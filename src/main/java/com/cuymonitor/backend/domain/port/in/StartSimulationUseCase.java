package com.cuymonitor.backend.domain.port.in;

import java.util.UUID;

/**
 * The dashboard's "Simulación" button: a demo of the real chain on one of the breeder's cages. The AI service is
 * fed a drawn cage where the first guinea pig looks sick, and the usual rules decide what to raise.
 */
public interface StartSimulationUseCase {

    void start(UUID requesterId, String cageCode);
}
