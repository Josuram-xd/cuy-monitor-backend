package com.cuymonitor.backend.domain.exception;

/** No account with that id. Answered with 404. */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
