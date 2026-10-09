package com.cuymonitor.backend.domain.health.chain;

import com.cuymonitor.backend.domain.model.BaselineProfile;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.port.out.BaselineProfileRepository;

import java.util.Objects;

public final class BehaviorThresholdHandler extends EventHandler {

    private final BaselineProfileRepository baselineProfileRepository;

    public BehaviorThresholdHandler(BaselineProfileRepository baselineProfileRepository) {
        this.baselineProfileRepository =
                Objects.requireNonNull(baselineProfileRepository, "baselineProfileRepository");
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
                .orElseGet(() -> BaselineProfile.defaultFor(guineaPig.getId()));
        context.useBaselineProfile(baseline);

        var reason = baseline.anomalyReason(behavior);
        if (reason != null) {
            context.markAnomaly(reason);
        }
    }
}
