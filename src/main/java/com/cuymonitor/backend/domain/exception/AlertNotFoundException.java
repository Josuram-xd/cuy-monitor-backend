package com.cuymonitor.backend.domain.exception;

public class AlertNotFoundException extends RuntimeException {

    public AlertNotFoundException() {
        super("alert not found");
    }
}
