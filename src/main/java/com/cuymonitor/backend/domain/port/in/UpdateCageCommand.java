package com.cuymonitor.backend.domain.port.in;

import java.util.Objects;

/** Replaces the name and the location (a null or blank location removes it). */
public record UpdateCageCommand(String cageCode, String name, String location) {

    public UpdateCageCommand {
        Objects.requireNonNull(cageCode, "cageCode");
        Objects.requireNonNull(name, "name");
    }
}
