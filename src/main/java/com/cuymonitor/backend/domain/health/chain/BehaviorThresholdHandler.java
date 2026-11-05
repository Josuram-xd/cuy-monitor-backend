package com.cuymonitor.backend.domain.health.chain;

import com.cuymonitor.backend.domain.model.AnomalyThresholds;
import com.cuymonitor.backend.domain.model.BaselineProfile;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.port.out.BaselineProfileRepository;

import java.util.Objects;

public final class BehaviorThresholdHandler extends EventHandler {

    private final BaselineProfileRepository baselineProfileRepository;
    private final AnomalyThresholds thresholds;
    private final int minLearnedSamples;

    /** The starting thresholds, and any stored profile is trusted. */
    public BehaviorThresholdHandler(BaselineProfileRepository baselineProfileRepository) {
        this(baselineProfileRepository, AnomalyThresholds.defaults(), 0);
    }

    /**
     * @param minLearnedSamples a learned profile is only used once it comes from this many windows (Task 11.2);
     *                          before that the guinea pig is compared with the default profile
     */
    public BehaviorThresholdHandler(BaselineProfileRepository baselineProfileRepository, AnomalyThresholds thresholds,
                                    int minLearnedSamples) {
        this.baselineProfileRepository =
                Objects.requireNonNull(baselineProfileRepository, "baselineProfileRepository");
        this.thresholds = Objects.requireNonNull(thresholds, "thresholds");
        if (minLearnedSamples < 0) {
            throw new IllegalArgumentException("minLearnedSamples can't be negative");
        }
        this.minLearnedSamples = minLearnedSamples;
    }

    @Override
    protected void handleCurrent(EventHandlerContext context) {
        if (!(context.event().signal() instanceof BehaviorSignal behavior)) {
            return;
        }

        var guineaPig = context.guineaPig().orElse(null);
        if (guineaPig == null || guineaPig.getId() == null) {
            context.reject("Behavior event must be identified before threshold evaluation");
            return;
        }

        var baseline = baselineProfileRepository.findByGuineaPigId(guineaPig.getId())
                .filter(profile -> profile.isLearned(minLearnedSamples))
                .orElseGet(() -> BaselineProfile.defaultFor(guineaPig.getId()));
        context.useBaselineProfile(baseline);

        var reason = baseline.anomalyReason(behavior, thresholds);
        if (reason != null) {
            context.markAnomaly(reason);
        }
    }
}
