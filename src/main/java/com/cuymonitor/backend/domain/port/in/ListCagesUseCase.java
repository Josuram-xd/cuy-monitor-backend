package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.Cage;

import java.util.List;
import java.util.UUID;

public interface ListCagesUseCase {
    /** The active cages of the account, oldest first. */
    List<Cage> list(UUID ownerId);
}
