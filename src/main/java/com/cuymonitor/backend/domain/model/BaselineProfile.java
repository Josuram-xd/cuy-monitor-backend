package com.cuymonitor.backend.domain.model;

import java.time.Instant;

// what is normal for one guinea pig; until it is learned from real data (Task 11.1) a default is used
public record BaselineProfile(long guineaPigId, double avgStillSeconds, double avgFeederVisits,
                              double avgGroupDistance, Instant updatedAt) {

    static final double CLASSIFIER_THRESHOLD = 0.7;
    static final double STILL_FACTOR = 1.5;
    static final double GROUP_DISTANCE_FACTOR = 2.0;

    public static BaselineProfile defaultFor(long guineaPigId) {
        return new BaselineProfile(guineaPigId, 30, 1, 0.3, Instant.EPOCH);
    }

    public boolean isAnomalous(BehaviorSignal window) {
        return anomalyReason(window) != null;
    }

    // null when the window looks normal
    public String anomalyReason(BehaviorSignal window) {
        if (window.probAnomaly() >= CLASSIFIER_THRESHOLD) {
            return "el clasificador marcó la ventana como anómala";
        }
        if (window.stillSeconds() > avgStillSeconds * STILL_FACTOR && window.feederVisits() == 0) {
            return "estuvo quieto mucho más de lo normal y no fue al comedero";
        }
        if (window.avgGroupDistance() > avgGroupDistance * GROUP_DISTANCE_FACTOR) {
            return "se alejó del grupo";
        }
        return null;
    }
}
