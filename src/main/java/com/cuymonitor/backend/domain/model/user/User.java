package com.cuymonitor.backend.domain.model.user;

import com.cuymonitor.backend.domain.exception.AccountDisabledException;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class User {

    public static final int MAX_NICKNAME_LENGTH = 30;

    private final UUID id;
    private final String username;
    private String fullName;
    private final String email;
    // null for an account that only signs in with Google
    private String passwordHash;
    private String googleSubject;
    private UserStatus status;
    private UserRole role;
    private String nickname;
    // only the type: the bytes are fetched on their own, so a list of accounts does not drag them along
    private String photoContentType;
    private final Instant createdAt;
    private Instant updatedAt;

    private User(UUID id, String username, String fullName, String email, String passwordHash,
                 String googleSubject, UserStatus status, UserRole role, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.username = requireText(username, "username");
        this.fullName = requireText(fullName, "fullName");
        this.email = requireText(email, "email");
        if (passwordHash == null && googleSubject == null) {
            throw new IllegalArgumentException("an account needs a password or a Google login");
        }
        this.passwordHash = passwordHash;
        this.googleSubject = googleSubject;
        this.status = Objects.requireNonNull(status, "status");
        this.role = Objects.requireNonNull(role, "role");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static User register(String username, String fullName, String email, String passwordHash, Instant now) {
        return new User(UUID.randomUUID(), normalize(username, "username"), requireText(fullName, "fullName").trim(),
                normalize(email, "email"), requireText(passwordHash, "passwordHash"), null,
                UserStatus.PENDING_VERIFICATION, UserRole.BREEDER, now, now);
    }

    /** Google already proved the email, so the account is active from the start and has no password. */
    public static User registerWithGoogle(String username, String fullName, String email, String googleSubject,
                                          Instant now) {
        return new User(UUID.randomUUID(), normalize(username, "username"), requireText(fullName, "fullName").trim(),
                normalize(email, "email"), null, requireText(googleSubject, "googleSubject"), UserStatus.ACTIVE,
                UserRole.BREEDER, now, now);
    }

    /**
     * Restores an ordinary breeder account. Shorthand for tests and for callers that have no role to restore:
     * it does not read a role, it <b>sets</b> BREEDER. Anything reading a stored account — the persistence
     * mapper above all — must use the overload that takes the role, or it quietly demotes an admin.
     */
    public static User restore(UUID id, String username, String fullName, String email, String passwordHash,
                               UserStatus status, Instant createdAt, Instant updatedAt) {
        return new User(id, username, fullName, email, passwordHash, null, status, UserRole.BREEDER, createdAt,
                updatedAt);
    }

    public static User restore(UUID id, String username, String fullName, String email, String passwordHash,
                               String googleSubject, UserStatus status, Instant createdAt, Instant updatedAt) {
        return new User(id, username, fullName, email, passwordHash, googleSubject, status, UserRole.BREEDER,
                createdAt, updatedAt);
    }

    /**
     * Ties this account to a Google identity. An account that was never verified by email may have been created
     * by someone else with a password only they know, so that password is wiped: only Google opens it now.
     */
    public void linkGoogle(String subject, Instant now) {
        ensureNotDisabled();
        requireText(subject, "googleSubject");
        if (googleSubject != null && !googleSubject.equals(subject)) {
            throw new IllegalStateException("the account is already linked to another Google identity");
        }
        googleSubject = subject;
        if (status == UserStatus.PENDING_VERIFICATION) {
            passwordHash = null;
            status = UserStatus.ACTIVE;
        }
        updatedAt = Objects.requireNonNull(now, "now");
    }

    public boolean hasPassword() {
        return passwordHash != null;
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

    /**
     * An admin switches a disabled account back on. {@link #activate(Instant)} refuses to, on purpose: that one
     * is the end of signing up, and an account disabled for a reason should not come back by verifying a code.
     */
    public void reinstate(Instant now) {
        if (status == UserStatus.DISABLED) {
            status = UserStatus.ACTIVE;
            updatedAt = Objects.requireNonNull(now, "now");
        }
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

    public String getGoogleSubject() {
        return googleSubject;
    }

    /** Restoring an account that knows its role: what the database actually has. */
    public static User restore(UUID id, String username, String fullName, String email, String passwordHash,
                               String googleSubject, UserStatus status, UserRole role, Instant createdAt,
                               Instant updatedAt) {
        return new User(id, username, fullName, email, passwordHash, googleSubject, status, role, createdAt,
                updatedAt);
    }

    /**
     * An admin changes another account's role. An account can't change its own, so the last admin can't lock
     * everybody out of the catalog by mistake; the service checks that before calling this.
     */
    public void changeRoleTo(UserRole newRole, Instant now) {
        role = Objects.requireNonNull(newRole, "newRole");
        updatedAt = Objects.requireNonNull(now, "now");
    }

    /**
     * Restoring an account with everything the database holds. The shorter overloads leave the nickname and
     * the photo empty, which is what an account that never set them looks like anyway.
     */
    public static User restore(UUID id, String username, String fullName, String email, String passwordHash,
                               String googleSubject, UserStatus status, UserRole role, String nickname,
                               String photoContentType, Instant createdAt, Instant updatedAt) {
        User user = new User(id, username, fullName, email, passwordHash, googleSubject, status, role, createdAt,
                updatedAt);
        user.nickname = nickname;
        user.photoContentType = photoContentType;
        return user;
    }

    /** The short name the breeder picked, or their full name when they have not picked one. */
    public String displayName() {
        return nickname == null ? fullName : nickname;
    }

    /**
     * Sets the short name, or clears it with null or blank. It is not an identifier: not unique, never used
     * to log in, so there is nothing to check beyond its length.
     */
    public void changeNickname(String newNickname, Instant now) {
        ensureNotDisabled();
        String trimmed = newNickname == null ? null : newNickname.trim();
        if (trimmed != null && trimmed.isEmpty()) {
            trimmed = null;
        }
        if (trimmed != null && trimmed.length() > MAX_NICKNAME_LENGTH) {
            throw new IllegalArgumentException("nickname must be at most " + MAX_NICKNAME_LENGTH + " characters");
        }
        nickname = trimmed;
        updatedAt = Objects.requireNonNull(now, "now");
    }

    /** The photo itself is written by the repository; the account only remembers that it has one. */
    public void attachPhoto(String newContentType, Instant now) {
        ensureNotDisabled();
        photoContentType = newContentType;
        updatedAt = Objects.requireNonNull(now, "now");
    }

    public boolean hasPhoto() {
        return photoContentType != null;
    }

    public String getNickname() {
        return nickname;
    }

    public String getPhotoContentType() {
        return photoContentType;
    }

    public boolean isAdmin() {
        return role.isAdmin();
    }

    public UserRole getRole() {
        return role;
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
