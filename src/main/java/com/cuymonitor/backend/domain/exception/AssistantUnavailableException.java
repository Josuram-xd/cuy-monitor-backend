package com.cuymonitor.backend.domain.exception;

/** The help assistant can't answer right now (turned off, the model failed or took too long). */
public class AssistantUnavailableException extends RuntimeException {

    public AssistantUnavailableException(String message) {
        super(message);
    }

    public AssistantUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
