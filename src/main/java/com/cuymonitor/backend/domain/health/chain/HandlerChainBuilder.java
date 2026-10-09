package com.cuymonitor.backend.domain.health.chain;

import java.util.Objects;

public final class HandlerChainBuilder {

    private final ValidationHandler validationHandler;
    private final IdentificationHandler identificationHandler;
    private final BehaviorThresholdHandler behaviorThresholdHandler;
    private final SustainedAnomalyHandler sustainedAnomalyHandler;

    public HandlerChainBuilder(
            ValidationHandler validationHandler,
            IdentificationHandler identificationHandler,
            BehaviorThresholdHandler behaviorThresholdHandler,
            SustainedAnomalyHandler sustainedAnomalyHandler
    ) {
        this.validationHandler = Objects.requireNonNull(validationHandler, "validationHandler");
        this.identificationHandler = Objects.requireNonNull(identificationHandler, "identificationHandler");
        this.behaviorThresholdHandler =
                Objects.requireNonNull(behaviorThresholdHandler, "behaviorThresholdHandler");
        this.sustainedAnomalyHandler =
                Objects.requireNonNull(sustainedAnomalyHandler, "sustainedAnomalyHandler");
    }

    public EventHandler build() {
        validationHandler.setNext(identificationHandler);
        identificationHandler.setNext(behaviorThresholdHandler);
        behaviorThresholdHandler.setNext(sustainedAnomalyHandler);
        return validationHandler;
    }
}
