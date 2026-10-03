package com.cuymonitor.backend.domain.exception;

/** Registration with a username or email that another account already uses. */
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException() {
        super("username or email already in use");
    }
}
