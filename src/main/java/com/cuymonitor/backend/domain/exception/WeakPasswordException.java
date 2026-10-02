package com.cuymonitor.backend.domain.exception;

/** The raw password does not follow {@code PasswordPolicy}. */
public class WeakPasswordException extends RuntimeException {

    public WeakPasswordException(String message) {
        super(message);
    }
}
