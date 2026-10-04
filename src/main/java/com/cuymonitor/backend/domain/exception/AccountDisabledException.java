package com.cuymonitor.backend.domain.exception;

/** The account was deactivated, or no longer exists, but someone still tries to use it. */
public class AccountDisabledException extends RuntimeException {

    public AccountDisabledException() {
        super("account is disabled");
    }
}
