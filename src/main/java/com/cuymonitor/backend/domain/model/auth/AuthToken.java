package com.cuymonitor.backend.domain.model.auth;

import java.time.Instant;
import java.util.Objects;

public record AuthToken(String accessToken, Instant expiresAt) {

    public AuthToken {
        Objects.requireNonNull(accessToken, "accessToken");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    @Override
    public String toString() {
        return "AuthToken[accessToken=***, expiresAt=" + expiresAt + "]";
    }
}
