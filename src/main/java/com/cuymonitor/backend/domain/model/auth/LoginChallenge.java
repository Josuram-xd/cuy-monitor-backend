package com.cuymonitor.backend.domain.model.auth;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Returned after register or login: the code was sent and must be verified with this id. */
public record LoginChallenge(UUID challengeId, Instant expiresAt) {

    public LoginChallenge {
        Objects.requireNonNull(challengeId, "challengeId");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }
}
