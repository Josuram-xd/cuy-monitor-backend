package com.cuymonitor.backend.domain.exception;

/**
 * The OTP code is wrong, expired, already used or out of attempts.
 * The message is always the same so callers cannot tell which one failed.
 */
public class InvalidOtpException extends RuntimeException {

    public InvalidOtpException() {
        super("invalid or expired code");
    }
}
