package com.cuymonitor.backend.domain.exception;

/** The admin asked for something the system must not do, such as removing the last admin. Answered with 400. */
public class InvalidAdminActionException extends RuntimeException {

    public InvalidAdminActionException(String message) {
        super(message);
    }
}
