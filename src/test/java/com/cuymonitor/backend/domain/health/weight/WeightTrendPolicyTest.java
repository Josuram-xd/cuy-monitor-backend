package com.cuymonitor.backend.domain.health.weight;

import com.cuymonitor.backend.domain.model.WeightReading;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeightTrendPolicyTest {

    private static final Instant NOW = Instant.parse("2026-11-05T12:00:00Z");
    private final WeightTrendPolicy policy = new WeightTrendPolicy(0.1, Duration.ofHours(24), Duration.ofDays(6), 3);

    private static WeightReading reading(double grams, boolean stable, Duration ago) {
        return new WeightReading(null, "cage-1", grams, stable, NOW.minus(ago));
    }

    private static List<WeightReading> days(double before, double recent) {
        List<WeightReading> readings = new ArrayList<>();
        for (int day = 2; day <= 5; day++) {
            readings.add(reading(before, true, Duration.ofDays(day)));
        }
        for (int hour = 2; hour <= 8; hour += 3) {
            readings.add(reading(recent, true, Duration.ofHours(hour)));
        }
        return readings;
    }

    @Test
    void aCageTenPercentLighterThanTheDaysBeforeIsGoingDown() {
        assertThat(policy.drop(days(2000, 1780), NOW)).hasValueSatisfying(
                drop -> assertThat(drop).isCloseTo(0.11, org.assertj.core.data.Offset.offset(1e-9)));
    }

    @Test
    void aSmallerDropOrAHeavierCageIsNot() {
        assertThat(policy.drop(days(2000, 1850), NOW)).isEmpty();
        assertThat(policy.drop(days(2000, 2300), NOW)).isEmpty();
    }

    @Test
    void unstableReadingsDoNotCount() {
        List<WeightReading> readings = new ArrayList<>(days(2000, 2000));
        // a guinea pig jumping off the platform: very low, but not stable
        for (int i = 0; i < 10; i++) {
            readings.add(reading(300, false, Duration.ofMinutes(10 + i)));
        }
        assertThat(policy.drop(readings, NOW)).isEmpty();
    }

    @Test
    void oneBadReadingDoesNotMoveTheMedian() {
        List<WeightReading> readings = new ArrayList<>(days(2000, 2000));
        readings.add(reading(500, true, Duration.ofHours(1)));
        assertThat(policy.drop(readings, NOW)).isEmpty();
    }

    @Test
    void withTooFewReadingsInAWindowNothingIsSaid() {
        List<WeightReading> onlyRecent = List.of(reading(1000, true, Duration.ofHours(1)),
                reading(1000, true, Duration.ofHours(2)), reading(1000, true, Duration.ofHours(3)));
        assertThat(policy.drop(onlyRecent, NOW)).isEmpty();
        assertThat(policy.drop(List.of(), NOW)).isEmpty();
    }

    @Test
    void theQueryStartsAtTheBeginningOfTheBaseline() {
        assertThat(policy.from(NOW)).isEqualTo(NOW.minus(Duration.ofDays(7)));
    }

    @Test
    void badSettingsAreRefused() {
        assertThatThrownBy(() -> new WeightTrendPolicy(0, Duration.ofHours(1), Duration.ofHours(1), 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new WeightTrendPolicy(0.1, Duration.ZERO, Duration.ofHours(1), 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new WeightTrendPolicy(0.1, Duration.ofHours(1), Duration.ofHours(1), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
