package com.cuymonitor.backend.domain.model.auth;

// The names of the claims of a session token. Both sides read them from here: the adapter that issues the
// token and the controller that reads it back, so neither has to know the other.
public final class TokenClaims {

    // the id of the login session the token belongs to; logging out revokes every token of that session
    public static final String SESSION = "sid";

    private TokenClaims() {
    }
}
