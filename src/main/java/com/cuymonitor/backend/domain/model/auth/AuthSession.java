package com.cuymonitor.backend.domain.model.auth;

import java.time.Instant;
import java.util.Objects;

/** Both tokens a successful login hands out. The refresh token is the raw value, only ever sent as a cookie. */
public record AuthSession(AuthToken accessToken, String refreshToken, Instant refreshExpiresAt) {

    public AuthSession {
        Objects.requireNonNull(accessToken, "accessToken");
        Objects.requireNonNull(refreshToken, "refreshToken");
        Objects.requireNonNull(refreshExpiresAt, "refreshExpiresAt");
    }

    @Override
    public String toString() {
        return "AuthSession[accessToken=***, refreshToken=***, refreshExpiresAt=" + refreshExpiresAt + "]";
    }
}
