package com.cuymonitor.backend.domain.exception;

import java.time.Duration;

/** Too many failed logins or code verifications; the caller has to wait {@link #getRetryAfter()}. */
public class TooManyAttemptsException extends RuntimeException {

    private final Duration retryAfter;

    public TooManyAttemptsException(Duration retryAfter) {
        super("too many attempts, try again later");
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
