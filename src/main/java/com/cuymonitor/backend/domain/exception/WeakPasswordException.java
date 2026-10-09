package com.cuymonitor.backend.domain.exception;

import java.util.List;

/** The raw password does not follow {@code PasswordPolicy}; rules lists the codes of the broken ones. */
public class WeakPasswordException extends RuntimeException {

    private final List<String> rules;

    public WeakPasswordException(String message) {
        this(message, List.of());
    }

    public WeakPasswordException(String message, List<String> rules) {
        super(message);
        this.rules = List.copyOf(rules);
    }

    public List<String> getRules() {
        return rules;
    }
}
