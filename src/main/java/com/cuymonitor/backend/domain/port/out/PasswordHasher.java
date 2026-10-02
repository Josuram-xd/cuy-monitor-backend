package com.cuymonitor.backend.domain.port.out;

public interface PasswordHasher {
    String hashPassword(String rawPassword);
    boolean matches(String rawPassword, String hashedPassword);
}
