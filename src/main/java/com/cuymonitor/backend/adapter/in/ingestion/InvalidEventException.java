package com.cuymonitor.backend.adapter.in.ingestion;

// the envelope or the payload breaks docs/contracts/events.md; the producer must not retry it
public class InvalidEventException extends RuntimeException {

    public InvalidEventException(String message) {
        super(message);
    }
}
