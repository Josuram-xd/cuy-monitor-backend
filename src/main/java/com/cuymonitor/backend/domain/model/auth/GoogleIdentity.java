package com.cuymonitor.backend.domain.model.auth;

import java.util.Objects;

/** What Google vouches for after its ID token was verified. subject is Google's stable user id. */
public record GoogleIdentity(String subject, String email, boolean emailVerified, String name) {

    public GoogleIdentity {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(email, "email");
    }
}
