package com.cuymonitor.backend.domain.exception;

/**
 * Wrong username or password, or a disabled account.
 * The message is always the same so callers cannot tell which one failed.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("invalid credentials");
    }
}
