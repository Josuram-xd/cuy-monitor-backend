package com.cuymonitor.backend.domain.port.in;

import java.util.Objects;
import java.util.UUID;

/** location is optional. */
public record CreateCageCommand(UUID ownerId, String name, String location) {

    public CreateCageCommand {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(name, "name");
    }
}
