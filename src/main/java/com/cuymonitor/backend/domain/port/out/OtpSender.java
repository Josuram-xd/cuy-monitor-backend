package com.cuymonitor.backend.domain.port.out;

import java.time.Instant;

public interface OtpSender {
    void sendOtp(String email, String code, Instant expiresAt);

}
