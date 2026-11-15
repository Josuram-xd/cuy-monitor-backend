package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.application.fake.MutableClock;
import com.cuymonitor.backend.domain.exception.TooManyAttemptsException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FailedAttemptLimiterTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-01T10:00:00Z"));
    private final FailedAttemptLimiter limiter = new FailedAttemptLimiter(Duration.ofMinutes(15), clock);

    @Test
    void blocksTheKeyOnceItReachesTheLimitAndSaysHowLongToWait() {
        limiter.recordFailure("a");
        limiter.recordFailure("a");
        assertThatCode(() -> limiter.check("a", 3)).doesNotThrowAnyException();

        limiter.recordFailure("a");
        clock.advance(Duration.ofMinutes(5));

        assertThatThrownBy(() -> limiter.check("a", 3))
                .isInstanceOfSatisfying(TooManyAttemptsException.class,
                        e -> assertThat(e.getRetryAfter()).isEqualTo(Duration.ofMinutes(10)));
        assertThatCode(() -> limiter.check("b", 3)).doesNotThrowAnyException();
    }

    @Test
    void theBlockEndsWithTheWindowAndResetClearsIt() {
        for (int i = 0; i < 3; i++) {
            limiter.recordFailure("a");
            limiter.recordFailure("b");
        }
        limiter.reset("b");
        clock.advance(Duration.ofMinutes(15));

        assertThatCode(() -> limiter.check("a", 3)).doesNotThrowAnyException();
        assertThatCode(() -> limiter.check("b", 3)).doesNotThrowAnyException();
    }
}
