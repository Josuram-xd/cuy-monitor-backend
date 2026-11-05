package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.model.AnomalyThresholds;
import com.cuymonitor.backend.domain.model.BaselineProfile;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.port.out.BaselineProfileRepository;

import java.time.Instant;
import java.util.Objects;

/**
 * Learns what is normal for each guinea pig (Task 11.1). It only learns from healthy behavior: a window that
 * already looks anomalous, or any window while the guinea pig is not NORMAL, is skipped, so the profile never
 * learns the behavior of a sick animal and starts to find it "normal". How to learn is the profile's own job
 * ({@link BaselineProfile#learnFrom}); this class decides when.
 */
public class BaselineLearner {

    private final BaselineProfileRepository baselines;
    private final AnomalyThresholds thresholds;
    private final int warmupSamples;

    public BaselineLearner(BaselineProfileRepository baselines, AnomalyThresholds thresholds, int warmupSamples) {
        this.baselines = Objects.requireNonNull(baselines, "baselines");
        this.thresholds = Objects.requireNonNull(thresholds, "thresholds");
        if (warmupSamples < 1) {
            throw new IllegalArgumentException("warmupSamples must be positive");
        }
        this.warmupSamples = warmupSamples;
    }

    /** @return true when the window was learned */
    public boolean learnFrom(GuineaPig guineaPig, BehaviorSignal window, Instant now) {
        if (guineaPig.getStatus() != HealthStatus.NORMAL) {
            return false;
        }
        BaselineProfile current = baselines.findByGuineaPigId(guineaPig.getId())
                .orElseGet(() -> BaselineProfile.defaultFor(guineaPig.getId()));
        if (current.isAnomalous(window, thresholds)) {
            return false;
        }
        baselines.save(current.learnFrom(window, now, warmupSamples));
        return true;
    }
}
