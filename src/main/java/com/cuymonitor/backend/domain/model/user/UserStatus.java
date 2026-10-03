package com.cuymonitor.backend.domain.model.user;

/** Lifecycle of an account. */
public enum UserStatus {
    /** Registered, but the email has not been confirmed with an OTP yet. */
    PENDING_VERIFICATION,
    ACTIVE,
    /** Soft-deleted by its owner. Kept for history, can no longer log in. */
    DISABLED
}
