package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.model.Cage;
import com.cuymonitor.backend.domain.model.GuineaPigPhoto;
import com.cuymonitor.backend.domain.port.in.GetOrCreateSimulationCageUseCase;
import com.cuymonitor.backend.domain.port.out.CageCodeGenerator;
import com.cuymonitor.backend.domain.port.out.CageRepository;
import com.cuymonitor.backend.domain.port.out.SimulationRunner;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The account's simulation cage: one per account, created on the first request with one guinea pig per mark color of
 * the video clip. Its code starts with {@link Cage#SIMULATION_PREFIX}, which is how the rest of the system knows it.
 * The colors are asked from the AI service before anything is written, so a failed request leaves nothing behind.
 */
public class SimulationCageService implements GetOrCreateSimulationCageUseCase {

    static final String NAME = "Simulación";

    // a neutral picture for the simulated guinea pigs, which have no real photo (a 32 x 32 PNG)
    static final byte[] PLACEHOLDER_PHOTO = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAIAAAD8GO2jAAAALElEQVR4nGPYsbKfpohh1IJRC0YtGLVg1IJRC0YtGLVg1IJRC0YtWDlELAAAcRHAammzAusAAAAASUVORK5CYII=");

    private final CageRepository cages;
    private final CageCodeGenerator codes;
    private final SimulationRunner runner;
    private final CageOperations cageOperations;
    private final Clock clock;

    public SimulationCageService(CageRepository cages, CageCodeGenerator codes, SimulationRunner runner,
                                 CageOperations cageOperations, Clock clock) {
        this.cages = Objects.requireNonNull(cages, "cages");
        this.codes = Objects.requireNonNull(codes, "codes");
        this.runner = Objects.requireNonNull(runner, "runner");
        this.cageOperations = Objects.requireNonNull(cageOperations, "cageOperations");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    @Transactional
    public Cage getOrCreate(UUID requesterId) {
        Optional<Cage> existing = cages.findActiveByOwner(requesterId).stream().filter(Cage::simulated).findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }
        List<String> colors = runner.clipColors();
        Cage cage = cages.save(Cage.create(codes.newCode(), NAME, null, requesterId, clock.instant()));
        // through the proxy: the owner check passes because the cage was just saved for this account
        for (String color : colors) {
            cageOperations.registerGuineaPig(requesterId, cage.code(), color,
                    new GuineaPigPhoto(PLACEHOLDER_PHOTO, "image/png"), null);
        }
        return cage;
    }
}
