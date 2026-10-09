package com.cuymonitor.backend.domain.exception;

/** The Google ID token is not valid: bad signature, wrong audience, expired or unverified email. */
public class InvalidGoogleTokenException extends RuntimeException {

    public InvalidGoogleTokenException() {
        super("invalid google token");
    }
}
