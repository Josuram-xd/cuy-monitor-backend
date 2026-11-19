package com.cuymonitor.backend.domain.exception;

/** The public demo account can be looked at and used, but not changed: anyone on the login page can open it. */
public class DemoAccountReadOnlyException extends RuntimeException {

    public DemoAccountReadOnlyException() {
        super("the demo account can not be changed");
    }
}
