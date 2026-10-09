package com.cuymonitor.backend.domain.exception;

/** The guinea pig does not exist in that cage, or was already deleted. */
public class GuineaPigNotFoundException extends RuntimeException {

    public GuineaPigNotFoundException() {
        super("guinea pig not found");
    }
}
