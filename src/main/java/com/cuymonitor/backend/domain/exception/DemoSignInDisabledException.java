package com.cuymonitor.backend.domain.exception;

/** No demo account is configured (or it is gone, disabled or an admin), so "Iniciar como usuario normal" is off. */
public class DemoSignInDisabledException extends RuntimeException {

    public DemoSignInDisabledException() {
        super("the demo account is not available");
    }
}
