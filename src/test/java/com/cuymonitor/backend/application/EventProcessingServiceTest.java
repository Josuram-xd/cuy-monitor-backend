package com.cuymonitor.backend.application;

import com.cuymonitor.backend.application.fake.MutableClock;
import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.health.chain.ChainSettings;
import com.cuymonitor.backend.domain.health.chain.HandlerChainBuilder;
import com.cuymonitor.backend.domain.health.fake.Events;
import com.cuymonitor.backend.domain.health.fake.HealthFakes;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AudioLabel;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.MarkColor;
import com.cuymonitor.backend.domain.model.WeightSignal;
import com.cuymonitor.backend.domain.notification.AlertPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventProcessingServiceTest {

    private static final Instant START = Instant.parse("2026-10-05T14:00:00Z");

    private final HealthFakes.GuineaPigs guineaPigs = new HealthFakes.GuineaPigs();
    private final HealthFakes.Events events = new HealthFakes.Events();
    private final HealthFakes.Transitions transitions = new HealthFakes.Transitions();
    private final HealthFakes.Weights weights = new HealthFakes.Weights();
    private final HealthFakes.RecordingObserver observer = new HealthFakes.RecordingObserver();
    private final MutableClock clock = new MutableClock(START);
    private EventProcessingService service;
    private long canelaId;

    @BeforeEach
    void setUp() {
        AlertPublisher publisher = new AlertPublisher();
        publisher.subscribe(observer);
        var chain = new HandlerChainBuilder(clock, guineaPigs, new HealthFakes.Baselines(), events,
                new ChainSettings(Duration.ofHours(1), 0.5, 3)).build();
        service = new EventProcessingService(new HealthFakes.Cages(), guineaPigs, events, transitions, weights,
                chain, publisher, clock);
        canelaId = guineaPigs.save(GuineaPig.register("cage-1", "Canela", MarkColor.RED, START)).getId();
    }

    @Test
    void threeAnomalousWindowsPutTheGuineaPigUnderObservation() {
        anomalousWindows(3);

        assertThat(status()).isEqualTo(HealthStatus.OBSERVED);
        assertThat(transitions.saved).hasSize(1);
        assertThat(observer.received).isEmpty();
    }

    @Test
    void anomalyThatKeepsGoingEscalatesToAlertAndWarns() {
        anomalousWindows(4);

        assertThat(status()).isEqualTo(HealthStatus.ALERT);
        Alert alert = observer.received.getFirst();
        assertThat(alert.getGuineaPigId()).isEqualTo(canelaId);
        assertThat(alert.getMessage()).startsWith("Canela está en alerta");
    }

    @Test
    void normalWindowsAfterAnAlertBringTheGuineaPigBack() {
        anomalousWindows(4);
        normalWindows(3);

        assertThat(status()).isEqualTo(HealthStatus.NORMAL);
        assertThat(transitions.saved).extracting(t -> t.toStatus())
                .containsExactly(HealthStatus.OBSERVED, HealthStatus.ALERT, HealthStatus.NORMAL);
    }

    @Test
    void aRetriedEventIsProcessedOnlyOnce() {
        HealthEvent event = Events.anomalousWindow(MarkColor.RED, START);

        service.process(event);
        service.process(event);

        assertThat(events.count()).isEqualTo(1);
    }

    @Test
    void droppedEventsAreNotStored() {
        service.process(Events.normalWindow(MarkColor.BLUE, START));

        assertThat(events.count()).isZero();
    }

    @Test
    void unknownCageIsRejected() {
        HealthEvent event = new HealthEvent(UUID.randomUUID(), "cage-9", START, "arduino", new WeightSignal(800, true));

        assertThatThrownBy(() -> service.process(event)).isInstanceOf(CageNotFoundException.class);
    }

    @Test
    void distressCallsRaiseOneCageAlertNotOnePerClip() {
        service.process(Events.audio(AudioLabel.DISTRESS, 0.9, START));
        clock.advance(Duration.ofSeconds(10));
        service.process(Events.audio(AudioLabel.DISTRESS, 0.95, clock.instant()));

        assertThat(observer.received).hasSize(1);
        Alert alert = observer.received.getFirst();
        assertThat(alert.getType()).isEqualTo(EventType.AUDIO);
        assertThat(alert.getGuineaPigId()).isNull();
    }

    @Test
    void weightEventsAreStoredAsReadings() {
        service.process(Events.weight(812.4, START));

        assertThat(weights.saved).singleElement().satisfies(r -> {
            assertThat(r.grams()).isEqualTo(812.4);
            assertThat(r.measuredAt()).isEqualTo(START);
        });
    }

    private void anomalousWindows(int count) {
        for (int i = 0; i < count; i++) {
            clock.advance(Duration.ofSeconds(60));
            service.process(Events.anomalousWindow(MarkColor.RED, clock.instant()));
        }
    }

    private void normalWindows(int count) {
        for (int i = 0; i < count; i++) {
            clock.advance(Duration.ofSeconds(60));
            service.process(Events.normalWindow(MarkColor.RED, clock.instant()));
        }
    }

    private HealthStatus status() {
        return guineaPigs.findById(canelaId).orElseThrow().getStatus();
    }
}
