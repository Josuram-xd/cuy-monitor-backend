package com.cuymonitor.backend.domain.health.chain;

import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.port.out.GuineaPigRepository;

// audio and weight are cage-level, so only BEHAVIOR events are tied to a guinea pig
public class IdentificationHandler extends EventHandler {

    private final GuineaPigRepository guineaPigRepository;

    public IdentificationHandler(GuineaPigRepository guineaPigRepository) {
        this.guineaPigRepository = guineaPigRepository;
    }

    @Override
    protected void process(EventContext context) {
        if (!(context.event().signal() instanceof BehaviorSignal window)) {
            return;
        }
        guineaPigRepository.findActiveByCageAndColor(context.event().cageId(), window.color())
                .ifPresentOrElse(context::identify,
                        () -> context.drop("no guinea pig registered with color " + window.color()));
    }
}
