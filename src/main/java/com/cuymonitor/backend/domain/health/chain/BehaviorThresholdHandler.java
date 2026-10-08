package com.cuymonitor.backend.domain.health.chain;

import com.cuymonitor.backend.domain.model.BaselineProfile;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.port.out.BaselineProfileRepository;

public class BehaviorThresholdHandler extends EventHandler {

    private final BaselineProfileRepository baselineProfileRepository;

    public BehaviorThresholdHandler(BaselineProfileRepository baselineProfileRepository) {
        this.baselineProfileRepository = baselineProfileRepository;
    }

    @Override
    protected void process(EventContext context) {
        if (!(context.event().signal() instanceof BehaviorSignal window) || context.guineaPig().isEmpty()) {
            return;
        }
        GuineaPig pig = context.guineaPig().get();
        BaselineProfile baseline = baselineProfileRepository.findByGuineaPigId(pig.getId())
                .orElseGet(() -> BaselineProfile.defaultFor(pig.getId()));
        context.useBaseline(baseline);

        String reason = baseline.anomalyReason(window);
        if (reason != null) {
            context.markAnomalous(reason);
        }
    }
}
