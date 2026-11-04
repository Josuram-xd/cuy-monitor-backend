package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.health.chain.EventHandler;
import com.cuymonitor.backend.domain.health.chain.EventHandlerContext;
import com.cuymonitor.backend.domain.health.state.GuineaPigHealthContext;
import com.cuymonitor.backend.domain.health.state.HealthStates;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AudioLabel;
import com.cuymonitor.backend.domain.model.AudioSignal;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.StateTransition;
import com.cuymonitor.backend.domain.notification.AlertPublisher;
import com.cuymonitor.backend.domain.port.in.ProcessEventUseCase;
import com.cuymonitor.backend.domain.port.in.RecordEventUseCase;
import com.cuymonitor.backend.domain.port.out.GuineaPigRepository;
import com.cuymonitor.backend.domain.port.out.StateTransitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * What happens to every event that arrives (GRASP Controller of the ingestion use case):
 *
 * <pre>
 * record it once ─► Chain of Responsibility ─► State of the guinea pig ─► transition ─► AlertPublisher
 * </pre>
 *
 * An alert is never saved here: it is published (Observer) and the subscribers store it, send it to the
 * dashboard over WebSocket and log it. The cage status itself is not stored: {@code CageHealthService} rebuilds it from the parts with the
 * Composite whenever it is asked.
 */
public class EventProcessingService implements ProcessEventUseCase {

    private static final Logger log = LoggerFactory.getLogger(EventProcessingService.class);

    // the breeder reads these in the dashboard, so they are in Spanish like the rest of the interface
    private static final String ALERT_MESSAGE = "Comportamiento fuera de lo normal de forma sostenida";
    private static final String CRITICAL_MESSAGE = "El comportamiento fuera de lo normal empeoró y sigue";
    private static final String DISTRESS_MESSAGE = "Se escucharon chillidos de angustia en la jaula";

    private final RecordEventUseCase recorder;
    private final EventHandler chain;
    private final GuineaPigRepository guineaPigs;
    private final StateTransitionRepository transitions;
    private final AlertPublisher alertPublisher;
    private final Clock clock;
    private final double distressProbability;

    public EventProcessingService(RecordEventUseCase recorder, EventHandler chain, GuineaPigRepository guineaPigs,
                                  StateTransitionRepository transitions, AlertPublisher alertPublisher, Clock clock,
                                  double distressProbability) {
        this.recorder = Objects.requireNonNull(recorder, "recorder");
        this.chain = Objects.requireNonNull(chain, "chain");
        this.guineaPigs = Objects.requireNonNull(guineaPigs, "guineaPigs");
        this.transitions = Objects.requireNonNull(transitions, "transitions");
        this.alertPublisher = Objects.requireNonNull(alertPublisher, "alertPublisher");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.distressProbability = distressProbability;
    }

    @Override
    @Transactional
    public void process(HealthEvent event) {
        if (!recorder.record(event)) {
            return;
        }
        EventHandlerContext context = chain.handle(new EventHandlerContext(event));
        if (!context.accepted()) {
            log.info("Event {} not evaluated: {}", event.eventId(), context.rejectionReason().orElse("rejected"));
            return;
        }
        Instant now = clock.instant();
        if (event.signal() instanceof AudioSignal audio) {
            evaluateAudio(event, audio, now);
            return;
        }
        context.guineaPig().ifPresent(pig -> evaluateGuineaPig(pig, context, now));
    }

    // State pattern: the current state of the guinea pig decides the next one
    private void evaluateGuineaPig(GuineaPig pig, EventHandlerContext context, Instant now) {
        var health = new GuineaPigHealthContext(pig.getId(), HealthStates.of(pig.getStatus()));
        Optional<StateTransition> transition = context.anomalyReason()
                .map(reason -> health.anomalyConfirmed(reason, now))
                .orElseGet(() -> health.recovered(now));
        transition.ifPresent(moved -> {
            transitions.save(moved);
            pig.changeStatus(moved.toStatus(), now);
            guineaPigs.save(pig);
            // the state decides: only a move up into ALERT or CRITICAL warns the breeder, a recovery does not
            if (health.raisesAlert() && moved.toStatus().compareTo(moved.fromStatus()) > 0) {
                alertPublisher.publish(Alert.forGuineaPig(pig, moved.toStatus(), messageFor(moved.toStatus()), now));
            }
        });
    }

    // the audio is a cage-level signal: a confident distress clip is an alert of the whole cage
    private void evaluateAudio(HealthEvent event, AudioSignal audio, Instant now) {
        if (audio.label() == AudioLabel.DISTRESS && audio.probability() >= distressProbability) {
            alertPublisher.publish(
                    Alert.forCage(event.cageId(), EventType.AUDIO, HealthStatus.ALERT, DISTRESS_MESSAGE, now));
        }
    }

    private static String messageFor(HealthStatus status) {
        return status == HealthStatus.CRITICAL ? CRITICAL_MESSAGE : ALERT_MESSAGE;
    }
}
