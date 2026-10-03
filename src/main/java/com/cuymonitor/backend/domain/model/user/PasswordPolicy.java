package com.cuymonitor.backend.domain.model.user;

import com.cuymonitor.backend.domain.exception.WeakPasswordException;
import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new WeakPasswordException("password is required");
        }
        if (rawPassword.length() < MIN_LENGTH) {
            throw new WeakPasswordException("password must have at least " + MIN_LENGTH + " characters");
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new WeakPasswordException("password must have at most " + MAX_BYTES + " bytes");
        }
    }
}
