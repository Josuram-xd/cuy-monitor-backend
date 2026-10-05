package com.cuymonitor.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(Jwt jwt, Otp otp) {

    public record Jwt(String secret, String issuer, Duration ttl) {

        private static final int MIN_SECRET_BYTES = 32;

        public Jwt {
            if (secret == null || secret.isBlank()) {
                throw new IllegalArgumentException("app.auth.jwt.secret is required (set APP_JWT_SECRET)");
            }
            // HS256 needs a key of at least 256 bits
            if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
                throw new IllegalArgumentException("app.auth.jwt.secret must have at least " + MIN_SECRET_BYTES + " bytes");
            }
            if (issuer == null || issuer.isBlank()) {
                throw new IllegalArgumentException("app.auth.jwt.issuer is required");
            }
            requirePositive(ttl, "app.auth.jwt.ttl");
        }
    }

    public record Otp(Duration ttl, int maxAttempts) {

        public Otp {
            requirePositive(ttl, "app.auth.otp.ttl");
            if (maxAttempts <= 0) {
                throw new IllegalArgumentException("app.auth.otp.max-attempts must be positive");
            }
        }
    }

    private static void requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be a positive duration");
        }
    }
}
