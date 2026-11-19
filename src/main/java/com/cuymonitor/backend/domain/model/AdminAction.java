package com.cuymonitor.backend.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** One admin change that went through: when, which admin, and what it did ("disable account …"). */
public record AdminAction(Instant at, UUID adminId, String description) {

    public AdminAction {
        Objects.requireNonNull(at, "at");
        Objects.requireNonNull(adminId, "adminId");
        Objects.requireNonNull(description, "description");
    }
}
