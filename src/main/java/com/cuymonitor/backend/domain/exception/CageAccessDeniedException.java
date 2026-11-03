package com.cuymonitor.backend.domain.exception;

/** The cage exists but belongs to another account (raised by the cage access proxy). */
public class CageAccessDeniedException extends RuntimeException {

    public CageAccessDeniedException() {
        super("this cage belongs to another account");
    }
}
