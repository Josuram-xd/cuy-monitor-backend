package com.cuymonitor.backend.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A cage of a farmer. code is the public id ("cage-1") used in events, URLs and WebSocket topics; ownerId is the
 * account that created it (null only for a pilot cage of a database without accounts). A deleted cage is kept with
 * active = false so its events, alerts and history stay.
 */
public record Cage(Long id, String code, String name, String location, UUID ownerId, boolean active,
                   Instant createdAt) {

    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_LOCATION_LENGTH = 200;

    public Cage {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
    }

    /** A cage as the older code knew it: no location, no owner, active. */
    public Cage(Long id, String code, String name) {
        this(id, code, name, null, null, true, null);
    }

    /** A new cage of an account, not stored yet. */
    public static Cage create(String code, String name, String location, UUID ownerId, Instant now) {
        Objects.requireNonNull(ownerId, "ownerId");
        return new Cage(null, code, cleanName(name), cleanLocation(location), ownerId, true, now);
    }

    public boolean isOwnedBy(UUID accountId) {
        return ownerId != null && ownerId.equals(accountId);
    }

    public Cage rename(String newName, String newLocation) {
        return new Cage(id, code, cleanName(newName), cleanLocation(newLocation), ownerId, active, createdAt);
    }

    public Cage deactivate() {
        return new Cage(id, code, name, location, ownerId, false, createdAt);
    }

    private static String cleanName(String name) {
        Objects.requireNonNull(name, "name");
        String trimmed = name.strip();
        if (trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("cage name must have 1 to " + MAX_NAME_LENGTH + " characters");
        }
        return trimmed;
    }

    // an empty location is the same as no location
    private static String cleanLocation(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }
        String trimmed = location.strip();
        if (trimmed.length() > MAX_LOCATION_LENGTH) {
            throw new IllegalArgumentException("cage location must have at most " + MAX_LOCATION_LENGTH + " characters");
        }
        return trimmed;
    }
}
