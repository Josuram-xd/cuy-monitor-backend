package com.cuymonitor.backend.domain.exception;

/** The account asking is not an admin. Same shape as CageAccessDeniedException, and answered with 403. */
public class AdminAccessDeniedException extends RuntimeException {

    public AdminAccessDeniedException(String message) {
        super(message);
    }
}
