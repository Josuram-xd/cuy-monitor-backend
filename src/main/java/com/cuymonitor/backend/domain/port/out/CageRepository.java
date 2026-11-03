package com.cuymonitor.backend.domain.port.out;

import com.cuymonitor.backend.domain.model.Cage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CageRepository {

    /** Only active cages: a deleted cage no longer receives events nor shows in the dashboard. */
    Optional<Cage> findByCode(String code);

    /** The active cages of an account, oldest first. */
    List<Cage> findActiveByOwner(UUID ownerId);

    boolean existsByCode(String code);

    Cage save(Cage cage);
}
