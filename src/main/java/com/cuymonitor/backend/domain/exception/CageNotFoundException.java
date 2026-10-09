package com.cuymonitor.backend.domain.exception;

public class CageNotFoundException extends RuntimeException {

    public CageNotFoundException(String cageCode) {
        super("unknown cage " + cageCode);
    }
}
