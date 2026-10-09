package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.WeightReading;
import com.cuymonitor.backend.domain.model.WeightSignal;
import com.cuymonitor.backend.domain.port.in.ProcessEventUseCase;
import com.cuymonitor.backend.domain.port.out.CageRepository;
import com.cuymonitor.backend.domain.port.out.EventRepository;
import com.cuymonitor.backend.domain.port.out.GuineaPigRepository;
import com.cuymonitor.backend.domain.port.out.WeightReadingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Stores what arrives: the raw event and, for WEIGHT, the reading. It does NOT decide anything about health
 * (no handler chain, no state change, no alerts). The team's EventProcessingService (Chain, State, Composite,
 * Observer) is meant to replace this class: delete it and its bean in DashboardConfig when that one is merged.
 * Until then the backend can start, receive events and show the last audio and weight values.
 */
public class EventIngestionService implements ProcessEventUseCase {

    private static final Logger log = LoggerFactory.getLogger(EventIngestionService.class);

    private final CageRepository cages;
    private final GuineaPigRepository guineaPigs;
    private final EventRepository events;
    private final WeightReadingRepository weights;

    public EventIngestionService(CageRepository cages, GuineaPigRepository guineaPigs, EventRepository events,
                                 WeightReadingRepository weights) {
        this.cages = cages;
        this.guineaPigs = guineaPigs;
        this.events = events;
        this.weights = weights;
    }

    @Override
    @Transactional
    public void process(HealthEvent event) {
        if (cages.findByCode(event.cageId()).isEmpty()) {
            throw new IllegalArgumentException("Unknown cage code: " + event.cageId());
        }
        // producers retry: the same eventId must not be stored twice
        if (events.existsById(event.eventId())) {
            log.debug("Ignoring duplicated event {}", event.eventId());
            return;
        }
        Long guineaPigId = null;
        if (event.signal() instanceof BehaviorSignal behavior) {
            Optional<GuineaPig> guineaPig = guineaPigs.findActiveByCageAndColor(event.cageId(), behavior.color());
            if (guineaPig.isEmpty()) {
                // a mark color nobody wears: nothing to attach the window to
                log.info("Ignoring BEHAVIOR event {}: no guinea pig with color {} in {}", event.eventId(),
                        behavior.color(), event.cageId());
                return;
            }
            guineaPigId = guineaPig.get().getId();
        }
        events.save(event, guineaPigId);
        if (event.signal() instanceof WeightSignal weight) {
            weights.save(new WeightReading(null, event.cageId(), weight.grams(), weight.stable(), event.occurredAt()));
        }
    }
}
