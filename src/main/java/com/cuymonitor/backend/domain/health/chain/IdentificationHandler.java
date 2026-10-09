package com.cuymonitor.backend.domain.health.chain;

import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.port.out.GuineaPigRepository;

import java.util.Objects;

public final class IdentificationHandler extends EventHandler {

    private final GuineaPigRepository guineaPigRepository;

    public IdentificationHandler(GuineaPigRepository guineaPigRepository) {
        this.guineaPigRepository = Objects.requireNonNull(guineaPigRepository, "guineaPigRepository");
    }

    @Override
    protected void handleCurrent(EventHandlerContext context) {
        if (!(context.event().signal() instanceof BehaviorSignal behavior)) {
            return;
        }

        var guineaPig = guineaPigRepository
                .findActiveByCageAndColor(context.event().cageId(), behavior.color());
        if (guineaPig.isEmpty() || guineaPig.get().getId() == null) {
            context.reject("No active registered guinea pig matches this cage and mark color");
            return;
        }

        context.identify(guineaPig.get());
    }
}
