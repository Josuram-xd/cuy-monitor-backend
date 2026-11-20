package com.cuymonitor.backend.domain.exception;

/** The camera settings or the agent's report are not acceptable. Answered with 400. */
public class InvalidCameraSettingsException extends RuntimeException {

    public InvalidCameraSettingsException(String message) {
        super(message);
    }
}
