package com.cuymonitor.backend.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * What is normal for one guinea pig, learned from its own normal windows (Task 11.1). GRASP Information Expert:
 * the profile knows how to compare a window with itself and how to learn from one.
 *
 * <p>Learning: while it has few windows it keeps their plain average, so the first ones count fully; once it has
 * {@code warmupSamples}, every new window moves it by 1/warmupSamples (a slow moving average), so it follows
 * slow changes of the animal but one odd window barely moves it.</p>
 *
 * @param sampleCount how many windows it was learned from; 0 for the default profile
 */
public record BaselineProfile(long guineaPigId, double avgStillSeconds, double avgFeederVisits,
                              double avgGroupDistance, Instant updatedAt, int sampleCount) {

    public BaselineProfile {
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (sampleCount < 0) {
            throw new IllegalArgumentException("sampleCount can't be negative");
        }
    }

    /** A profile not learned from any window (the default, or one stored before V13). */
    public BaselineProfile(long guineaPigId, double avgStillSeconds, double avgFeederVisits, double avgGroupDistance,
                           Instant updatedAt) {
        this(guineaPigId, avgStillSeconds, avgFeederVisits, avgGroupDistance, updatedAt, 0);
    }

    /** What a guinea pig is compared with before anything is learned about it. */
    public static BaselineProfile defaultFor(long guineaPigId) {
        return new BaselineProfile(guineaPigId, 30, 1, 0.3, Instant.EPOCH, 0);
    }

    /** True once it was learned from enough windows to be trusted over the default. */
    public boolean isLearned(int minSamples) {
        return sampleCount >= minSamples;
    }

    /** This profile after one more normal window. */
    public BaselineProfile learnFrom(BehaviorSignal window, Instant now, int warmupSamples) {
        if (warmupSamples < 1) {
            throw new IllegalArgumentException("warmupSamples must be positive");
        }
        int samples = sampleCount + 1;
        // the first window replaces the default; then a plain average until the warm-up, then a moving average
        double weight = 1.0 / Math.min(samples, warmupSamples);
        return new BaselineProfile(guineaPigId,
                moveTowards(avgStillSeconds, window.stillSeconds(), weight),
                moveTowards(avgFeederVisits, window.feederVisits(), weight),
                moveTowards(avgGroupDistance, window.avgGroupDistance(), weight),
                Objects.requireNonNull(now, "now"),
                samples);
    }

    public boolean isAnomalous(BehaviorSignal window, AnomalyThresholds thresholds) {
        return anomalyReason(window, thresholds) != null;
    }

    public boolean isAnomalous(BehaviorSignal window) {
        return isAnomalous(window, AnomalyThresholds.defaults());
    }

    // null when the window looks normal. Read by the breeder in the history, so in Spanish.
    public String anomalyReason(BehaviorSignal window, AnomalyThresholds thresholds) {
        if (window.probAnomaly() >= thresholds.classifierThreshold()) {
            return "el clasificador marcó la ventana como anómala";
        }
        if (window.stillSeconds() > avgStillSeconds * thresholds.stillFactor() && window.feederVisits() == 0) {
            return "estuvo quieto mucho más de lo normal y no fue al comedero";
        }
        if (window.avgGroupDistance() > avgGroupDistance * thresholds.groupDistanceFactor()) {
            return "se alejó del grupo";
        }
        return null;
    }

    public String anomalyReason(BehaviorSignal window) {
        return anomalyReason(window, AnomalyThresholds.defaults());
    }

    private static double moveTowards(double current, double value, double weight) {
        return current + (value - current) * weight;
    }
}
