package com.cuymonitor.backend.domain.exception;

/** The refresh token is missing, unknown, expired, revoked or its user can no longer log in. */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("invalid or expired session");
    }
}
