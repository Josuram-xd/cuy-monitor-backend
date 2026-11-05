package com.cuymonitor.backend.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeRangeTest {

    private static final Instant NOW = Instant.parse("2026-11-04T12:00:00Z");

    @Test
    void defaultsToTheLast24Hours() {
        TimeRange range = TimeRange.of(null, null, NOW);

        assertThat(range.to()).isEqualTo(NOW);
        assertThat(range.from()).isEqualTo(NOW.minus(Duration.ofHours(24)));
    }

    @Test
    void aMissingFromIs24HoursBeforeTo() {
        Instant to = NOW.minus(Duration.ofDays(2));

        assertThat(TimeRange.of(null, to, NOW).from()).isEqualTo(to.minus(Duration.ofHours(24)));
    }

    @Test
    void toMustBeAfterFrom() {
        assertThatThrownBy(() -> TimeRange.of(NOW, NOW, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TimeRange.of(NOW, NOW.minusSeconds(1), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allowsUpTo31DaysAndNoMore() {
        assertThat(TimeRange.of(NOW.minus(Duration.ofDays(31)), NOW, NOW)).isNotNull();
        assertThatThrownBy(() -> TimeRange.of(NOW.minus(Duration.ofDays(32)), NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
