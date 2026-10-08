package com.cuymonitor.backend.domain.health.chain;

import com.cuymonitor.backend.domain.port.out.BaselineProfileRepository;
import com.cuymonitor.backend.domain.port.out.EventRepository;
import com.cuymonitor.backend.domain.port.out.GuineaPigRepository;

import java.time.Clock;

// the only place that knows the order of the chain
public class HandlerChainBuilder {

    private final Clock clock;
    private final GuineaPigRepository guineaPigRepository;
    private final BaselineProfileRepository baselineProfileRepository;
    private final EventRepository eventRepository;
    private final ChainSettings settings;

    public HandlerChainBuilder(Clock clock, GuineaPigRepository guineaPigRepository,
                               BaselineProfileRepository baselineProfileRepository, EventRepository eventRepository,
                               ChainSettings settings) {
        this.clock = clock;
        this.guineaPigRepository = guineaPigRepository;
        this.baselineProfileRepository = baselineProfileRepository;
        this.eventRepository = eventRepository;
        this.settings = settings;
    }

    public EventHandler build() {
        EventHandler first = new ValidationHandler(clock, settings.maxEventAge(), settings.minDetectionConfidence());
        first.setNext(new IdentificationHandler(guineaPigRepository))
                .setNext(new BehaviorThresholdHandler(baselineProfileRepository))
                .setNext(new SustainedAnomalyHandler(eventRepository, settings.sustainedWindows()));
        return first;
    }
}
