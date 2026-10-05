package com.cuymonitor.backend.adapter.out.mail;

import com.cuymonitor.backend.domain.port.out.OtpSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;

// local development only: prints the code instead of sending an email
@Component
@Profile("dev")
public class LogOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(LogOtpSender.class);

    @Override
    public void sendOtp(String email, String code, Instant expiresAt) {
        log.info("OTP for {}: {} (expires at {})", email, code, expiresAt);
    }
}
