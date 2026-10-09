package com.cuymonitor.backend.domain.exception;

/** The cage code in the URL does not exist. */
public class CageNotFoundException extends RuntimeException {

    public CageNotFoundException() {
        super("cage not found");
    }
}
