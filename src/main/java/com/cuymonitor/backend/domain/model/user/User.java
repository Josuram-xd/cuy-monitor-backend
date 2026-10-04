package com.cuymonitor.backend.domain.model.user;

import com.cuymonitor.backend.domain.exception.AccountDisabledException;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class User {

    private final UUID id;
    private final String username;
    private String fullName;
    private final String email;
    private String passwordHash;
    private UserStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private User(UUID id, String username, String fullName, String email, String passwordHash,
                 UserStatus status, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.username = requireText(username, "username");
        this.fullName = requireText(fullName, "fullName");
        this.email = requireText(email, "email");
        this.passwordHash = requireText(passwordHash, "passwordHash");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static User register(String username, String fullName, String email, String passwordHash, Instant now) {
        return new User(UUID.randomUUID(), normalize(username, "username"), requireText(fullName, "fullName").trim(),
                normalize(email, "email"), passwordHash, UserStatus.PENDING_VERIFICATION, now, now);
    }

    public static User restore(UUID id, String username, String fullName, String email, String passwordHash,
                               UserStatus status, Instant createdAt, Instant updatedAt) {
        return new User(id, username, fullName, email, passwordHash, status, createdAt, updatedAt);
    }

    public void activate(Instant now) {
        if (status == UserStatus.DISABLED) {
            throw new IllegalStateException("a disabled account cannot be activated");
        }
        if (status == UserStatus.PENDING_VERIFICATION) {
            status = UserStatus.ACTIVE;
            updatedAt = Objects.requireNonNull(now, "now");
        }
    }

    public boolean canLogIn() {
        return status != UserStatus.DISABLED;
    }

    public void updateProfile(String newFullName, Instant now) {
        ensureNotDisabled();
        fullName = requireText(newFullName, "fullName").trim();
        updatedAt = Objects.requireNonNull(now, "now");
    }

    public void changePassword(String newPasswordHash, Instant now) {
        ensureNotDisabled();
        passwordHash = requireText(newPasswordHash, "passwordHash");
        updatedAt = Objects.requireNonNull(now, "now");
    }

    public void deactivate(Instant now) {
        ensureNotDisabled();
        status = UserStatus.DISABLED;
        updatedAt = Objects.requireNonNull(now, "now");
    }

    private void ensureNotDisabled() {
        if (status == UserStatus.DISABLED) {
            throw new AccountDisabledException();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static String normalize(String value, String name) {
        return requireText(value, name).trim().toLowerCase(Locale.ROOT);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
