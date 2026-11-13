package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.exception.SimulationUnavailableException;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.port.in.StartSimulationUseCase;
import com.cuymonitor.backend.domain.port.out.SimulationRunner;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Starts the demo on a cage. The guinea pigs are read through the {@link CageOperations} proxy, so only the owner
 * of the cage can start it, and the drawing uses the marks they really registered.
 */
public class SimulationService implements StartSimulationUseCase {

    private final CageOperations cageOperations;
    private final SimulationRunner runner;

    public SimulationService(CageOperations cageOperations, SimulationRunner runner) {
        this.cageOperations = Objects.requireNonNull(cageOperations, "cageOperations");
        this.runner = Objects.requireNonNull(runner, "runner");
    }

    @Override
    public void start(UUID requesterId, String cageCode) {
        List<String> colors = cageOperations.listGuineaPigs(requesterId, cageCode).stream()
                .map(GuineaPig::getMarkColor)
                .distinct()
                .toList();
        if (colors.isEmpty()) {
            throw SimulationUnavailableException.noGuineaPigs();
        }
        runner.start(cageCode, colors);
    }
}
