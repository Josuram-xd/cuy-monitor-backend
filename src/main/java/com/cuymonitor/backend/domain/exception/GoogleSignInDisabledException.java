package com.cuymonitor.backend.domain.exception;

/** The server has no Google client id configured, so signing in with Google is off. */
public class GoogleSignInDisabledException extends RuntimeException {

    public GoogleSignInDisabledException() {
        super("sign in with Google is not configured");
    }
}
