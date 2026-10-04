package com.cuymonitor.backend.domain.model.auth;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OtpChallengeTest {

    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(5);

    @Test
    void issueSetsExpirationFromTtl() {
        OtpChallenge otp = issue(5);

        assertThat(otp.getExpiresAt()).isEqualTo(NOW.plus(TTL));
        assertThat(otp.getAttempts()).isZero();
        assertThat(otp.getUsedAt()).isEmpty();
        assertThat(otp.getRevokedAt()).isEmpty();
    }

    @Test
    void issueRejectsInvalidTtlOrAttempts() {
        UUID userId = UUID.randomUUID();
        assertThatThrownBy(() -> OtpChallenge.issue(userId, "hash", NOW, Duration.ZERO, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OtpChallenge.issue(userId, "hash", NOW, TTL, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void correctCodeVerifiesAndMarksAsUsed() {
        OtpChallenge otp = issue(5);
        Instant later = NOW.plusSeconds(30);

        assertThat(otp.verify(true, later)).isEqualTo(OtpVerificationResult.VERIFIED);
        assertThat(otp.getUsedAt()).contains(later);
    }

    @Test
    void codeCanOnlyBeUsedOnce() {
        OtpChallenge otp = issue(5);
        otp.verify(true, NOW);

        assertThat(otp.verify(true, NOW)).isEqualTo(OtpVerificationResult.ALREADY_USED);
    }

    @Test
    void wrongCodeCountsAnAttempt() {
        OtpChallenge otp = issue(5);

        assertThat(otp.verify(false, NOW)).isEqualTo(OtpVerificationResult.WRONG_CODE);
        assertThat(otp.getAttempts()).isEqualTo(1);
        assertThat(otp.getUsedAt()).isEmpty();
    }

    @Test
    void expiresExactlyAtExpirationTime() {
        OtpChallenge otp = issue(5);

        assertThat(otp.verify(true, NOW.plus(TTL))).isEqualTo(OtpVerificationResult.EXPIRED);
    }

    @Test
    void runsOutOfAttemptsEvenWithTheCorrectCode() {
        OtpChallenge otp = issue(2);
        otp.verify(false, NOW);
        otp.verify(false, NOW);

        assertThat(otp.verify(true, NOW)).isEqualTo(OtpVerificationResult.TOO_MANY_ATTEMPTS);
        assertThat(otp.getAttempts()).isEqualTo(2);
    }

    @Test
    void revokedChallengeCannotBeVerified() {
        OtpChallenge otp = issue(5);
        otp.revoke(NOW);

        assertThat(otp.getRevokedAt()).contains(NOW);
        assertThat(otp.verify(true, NOW)).isEqualTo(OtpVerificationResult.ALREADY_USED);
    }

    @Test
    void revokeDoesNotTouchAnAlreadyUsedChallenge() {
        OtpChallenge otp = issue(5);
        otp.verify(true, NOW);

        otp.revoke(NOW.plusSeconds(10));

        assertThat(otp.getRevokedAt()).isEmpty();
    }

    private static OtpChallenge issue(int maxAttempts) {
        return OtpChallenge.issue(UUID.randomUUID(), "hash", NOW, TTL, maxAttempts);
    }
}
