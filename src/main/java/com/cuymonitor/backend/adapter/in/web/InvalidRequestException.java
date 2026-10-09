package com.cuymonitor.backend.adapter.in.web;

/** The request is well formed but asks for something the endpoint does not allow. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
