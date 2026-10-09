package com.cuymonitor.backend.domain.exception;

/** The user asked for too many OTP codes in a short time. */
public class TooManyOtpRequestsException extends RuntimeException {

    public TooManyOtpRequestsException() {
        super("too many codes requested, try again later");
    }
}
