package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.health.chain.EventContext;
import com.cuymonitor.backend.domain.health.chain.EventHandler;
import com.cuymonitor.backend.domain.health.composite.CageAudioHealth;
import com.cuymonitor.backend.domain.health.state.GuineaPigHealthContext;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AudioSignal;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.StateTransition;
import com.cuymonitor.backend.domain.model.WeightReading;
import com.cuymonitor.backend.domain.model.WeightSignal;
import com.cuymonitor.backend.domain.notification.AlertPublisher;
import com.cuymonitor.backend.domain.port.in.ProcessEventUseCase;
import com.cuymonitor.backend.domain.port.out.CageRepository;
import com.cuymonitor.backend.domain.port.out.EventRepository;
import com.cuymonitor.backend.domain.port.out.GuineaPigRepository;
import com.cuymonitor.backend.domain.port.out.StateTransitionRepository;
import com.cuymonitor.backend.domain.port.out.WeightReadingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

public class EventProcessingService implements ProcessEventUseCase {

    private static final Logger log = LoggerFactory.getLogger(EventProcessingService.class);

    private final CageRepository cageRepository;
    private final GuineaPigRepository guineaPigRepository;
    private final EventRepository eventRepository;
    private final StateTransitionRepository stateTransitionRepository;
    private final WeightReadingRepository weightReadingRepository;
    private final EventHandler chain;
    private final AlertPublisher alertPublisher;
    private final Clock clock;

    public EventProcessingService(CageRepository cageRepository, GuineaPigRepository guineaPigRepository,
                                  EventRepository eventRepository, StateTransitionRepository stateTransitionRepository,
                                  WeightReadingRepository weightReadingRepository, EventHandler chain,
                                  AlertPublisher alertPublisher, Clock clock) {
        this.cageRepository = cageRepository;
        this.guineaPigRepository = guineaPigRepository;
        this.eventRepository = eventRepository;
        this.stateTransitionRepository = stateTransitionRepository;
        this.weightReadingRepository = weightReadingRepository;
        this.chain = chain;
        this.alertPublisher = alertPublisher;
        this.clock = clock;
    }

    @Override
    public void process(HealthEvent event) {
        if (cageRepository.findByCode(event.cageId()).isEmpty()) {
            throw new CageNotFoundException(event.cageId());
        }
        // producers retry with the same eventId, so a repeated event is simply ignored
        if (eventRepository.existsById(event.eventId())) {
            return;
        }

        EventContext context = new EventContext(event);
        chain.handle(context);
        if (context.isDropped()) {
            log.debug("Dropped event id={}: {}", event.eventId(), context.dropReason());
            return;
        }

        Optional<HealthEvent> previousAudio = event.type() == EventType.AUDIO
                ? eventRepository.findLatest(event.cageId(), EventType.AUDIO)
                : Optional.empty();
        eventRepository.save(event, context.guineaPig().map(GuineaPig::getId).orElse(null));

        switch (event.signal()) {
            case BehaviorSignal window -> updateGuineaPigState(context);
            case AudioSignal clip -> checkCageAudio(event, previousAudio);
            case WeightSignal weight -> weightReadingRepository.save(new WeightReading(null, event.cageId(),
                    weight.grams(), weight.stable(), event.occurredAt()));
        }
    }

    private void updateGuineaPigState(EventContext context) {
        GuineaPig pig = context.guineaPig().orElseThrow();
        GuineaPigHealthContext health = new GuineaPigHealthContext(pig, stateTransitionRepository, alertPublisher, clock);

        Optional<StateTransition> transition = Optional.empty();
        if (context.isSustainedAnomaly()) {
            transition = health.onSustainedAnomaly("anomalía sostenida: " + context.anomalyReason());
        } else if (context.isSustainedRecovery()) {
            transition = health.onSustainedRecovery("volvió a su comportamiento normal");
        }
        transition.ifPresent(t -> guineaPigRepository.save(pig));
    }

    private void checkCageAudio(HealthEvent event, Optional<HealthEvent> previousAudio) {
        Instant now = clock.instant();
        HealthStatus before = new CageAudioHealth(previousAudio, now).status();
        HealthStatus after = new CageAudioHealth(Optional.of(event), now).status();
        // one alert when the cage starts sounding distressed, not one per clip
        if (after.raisesAlert() && after.isWorseThan(before)) {
            alertPublisher.publish(Alert.forCage(event.cageId(), EventType.AUDIO, after,
                    "Se escucharon chillidos de angustia en la jaula", now));
        }
    }
}
