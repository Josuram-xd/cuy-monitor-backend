package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.Cage;

import java.util.UUID;

/** The dashboard's "Jaula de simulación" button: the account's simulation cage, created on the first call. */
public interface GetOrCreateSimulationCageUseCase {

    Cage getOrCreate(UUID requesterId);
}
