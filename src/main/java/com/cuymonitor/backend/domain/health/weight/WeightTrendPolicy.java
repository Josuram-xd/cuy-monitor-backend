package com.cuymonitor.backend.domain.health.weight;

import com.cuymonitor.backend.domain.model.WeightReading;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * When the weight of a cage is going down (Task 12.2). Only <b>stable</b> readings count: a guinea pig stepping on
 * or off the platform gives readings that jump around and mean nothing.
 *
 * <pre>
 *  ───── baseline window ─────┼──── recent window ────┤ now
 *     median of these          │   median of these     │
 * </pre>
 *
 * The weight is going down when the recent median is at least {@code dropRatio} below the baseline median. With
 * fewer than {@code minReadings} stable readings in either window there is not enough to say anything. The median,
 * not the mean, so one bad reading (food spilled on the scale) does not move it.
 */
public record WeightTrendPolicy(double dropRatio, Duration recentWindow, Duration baselineWindow, int minReadings) {

    public WeightTrendPolicy {
        Objects.requireNonNull(recentWindow, "recentWindow");
        Objects.requireNonNull(baselineWindow, "baselineWindow");
        if (!(dropRatio > 0 && dropRatio < 1)) {
            throw new IllegalArgumentException("dropRatio must be between 0 and 1");
        }
        if (recentWindow.isNegative() || recentWindow.isZero() || baselineWindow.isNegative()
                || baselineWindow.isZero()) {
            throw new IllegalArgumentException("the weight windows must be positive");
        }
        if (minReadings < 1) {
            throw new IllegalArgumentException("minReadings must be at least 1");
        }
    }

    /** The earliest reading the policy needs, for the query. */
    public Instant from(Instant now) {
        return now.minus(recentWindow).minus(baselineWindow);
    }

    /** The drop as a ratio (0.12 = 12 % lighter) when the weight is going down; empty otherwise. */
    public Optional<Double> drop(List<WeightReading> readings, Instant now) {
        Instant recentFrom = now.minus(recentWindow);
        Instant baselineFrom = recentFrom.minus(baselineWindow);
        List<Double> recent = stableGrams(readings, recentFrom, now, true);
        List<Double> baseline = stableGrams(readings, baselineFrom, recentFrom, false);
        if (recent.size() < minReadings || baseline.size() < minReadings) {
            return Optional.empty();
        }
        double before = median(baseline);
        if (before <= 0) {
            return Optional.empty();
        }
        double ratio = (before - median(recent)) / before;
        return ratio >= dropRatio ? Optional.of(ratio) : Optional.empty();
    }

    // (from, to] for the recent window, [from, to) for the baseline: a reading is never in both
    private static List<Double> stableGrams(List<WeightReading> readings, Instant from, Instant to,
                                            boolean recent) {
        return readings.stream()
                .filter(WeightReading::stable)
                .filter(r -> recent
                        ? r.measuredAt().isAfter(from) && !r.measuredAt().isAfter(to)
                        : !r.measuredAt().isBefore(from) && r.measuredAt().isBefore(to))
                .map(WeightReading::grams)
                .sorted()
                .toList();
    }

    private static double median(List<Double> sorted) {
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 1 ? sorted.get(middle) : (sorted.get(middle - 1) + sorted.get(middle)) / 2;
    }
}
