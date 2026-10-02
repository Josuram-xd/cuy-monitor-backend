package com.cuymonitor.backend.domain.model.auth;

public enum OtpVerificationResult {
    VERIFIED,
    WRONG_CODE,
    EXPIRED,
    TOO_MANY_ATTEMPTS,
    ALREADY_USED
}
