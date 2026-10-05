package com.cuymonitor.backend.application.fake;

import com.cuymonitor.backend.domain.port.out.OtpSender;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class RecordingOtpSender implements OtpSender {

    public record SentOtp(String email, String code, Instant expiresAt) {
    }

    private final List<SentOtp> sent = new ArrayList<>();

    @Override
    public void sendOtp(String email, String code, Instant expiresAt) {
        sent.add(new SentOtp(email, code, expiresAt));
    }

    public List<SentOtp> sent() {
        return sent;
    }

    public String lastCode() {
        return sent.getLast().code();
    }
}
