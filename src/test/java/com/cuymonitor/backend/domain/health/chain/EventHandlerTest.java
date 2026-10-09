package com.cuymonitor.backend.domain.health.chain;

import com.cuymonitor.backend.domain.model.AudioLabel;
import com.cuymonitor.backend.domain.model.AudioSignal;
import com.cuymonitor.backend.domain.model.BaselineProfile;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.MarkColor;
import com.cuymonitor.backend.domain.model.WeightSignal;
import com.cuymonitor.backend.domain.port.out.BaselineProfileRepository;
import com.cuymonitor.backend.domain.port.out.EventRepository;
import com.cuymonitor.backend.domain.port.out.GuineaPigRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:32:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void validationRejectsStaleLowConfidenceAndMalformedEvents() {
        var handler = new ValidationHandler(CLOCK, Duration.ofMinutes(5), 0.5);

        var stale = handler.handle(new EventHandlerContext(
                behaviorEvent(UUID.randomUUID(), NOW.minus(Duration.ofMinutes(6)), 0.8, 0.9)
        ));
        var lowConfidence = handler.handle(new EventHandlerContext(
                behaviorEvent(UUID.randomUUID(), NOW, 0.8, 0.49)
        ));
        var malformed = handler.handle(new EventHandlerContext(
                event(UUID.randomUUID(), NOW, new WeightSignal(Double.NaN, true))
        ));

        assertFalse(stale.accepted());
        assertEquals("Event is stale", stale.rejectionReason().orElseThrow());
        assertFalse(lowConfidence.accepted());
        assertFalse(malformed.accepted());
    }

    @Test
    void validationPassesValidEventToTheNextHandler() {
        var downstreamCalls = new AtomicInteger();
        var validation = new ValidationHandler(CLOCK, Duration.ofMinutes(5), 0.5);
        validation.setNext(new EventHandler() {
            @Override
            protected void handleCurrent(EventHandlerContext context) {
                downstreamCalls.incrementAndGet();
            }
        });

        var context = validation.handle(new EventHandlerContext(
                behaviorEvent(UUID.randomUUID(), NOW, 0.8, 0.9)
        ));

        assertTrue(context.accepted());
        assertEquals(1, downstreamCalls.get());
    }

    @Test
    void rejectedEventDoesNotContinueThroughTheChain() {
        var downstreamCalls = new AtomicInteger();
        var validation = new ValidationHandler(CLOCK, Duration.ofMinutes(5), 0.5);
        validation.setNext(new EventHandler() {
            @Override
            protected void handleCurrent(EventHandlerContext context) {
                downstreamCalls.incrementAndGet();
            }
        });

        var context = validation.handle(new EventHandlerContext(
                behaviorEvent(UUID.randomUUID(), NOW.plusSeconds(1), 0.8, 0.9)
        ));

        assertFalse(context.accepted());
        assertEquals(0, downstreamCalls.get());
    }

    @Test
    void identificationMapsBehaviorColorToActiveGuineaPigAndSkipsCageEvents() {
        var guineaPig = guineaPig();
        var lookups = new AtomicInteger();
        var handler = new IdentificationHandler(guineaPigRepository(Optional.of(guineaPig), lookups));

        var behaviorContext = new EventHandlerContext(
                behaviorEvent(UUID.randomUUID(), NOW, 0.8, 0.9)
        );
        handler.handle(behaviorContext);
        var audioContext = new EventHandlerContext(
                event(UUID.randomUUID(), NOW, new AudioSignal(AudioLabel.NORMAL, 0.2, 960))
        );
        handler.handle(audioContext);

        assertSame(guineaPig, behaviorContext.guineaPig().orElseThrow());
        assertTrue(audioContext.accepted());
        assertTrue(audioContext.guineaPig().isEmpty());
        assertEquals(1, lookups.get());
    }

    @Test
    void identificationRejectsBehaviorWithoutAnActiveRegisteredGuineaPig() {
        var handler = new IdentificationHandler(guineaPigRepository(Optional.empty(), new AtomicInteger()));

        var context = handler.handle(new EventHandlerContext(
                behaviorEvent(UUID.randomUUID(), NOW, 0.8, 0.9)
        ));

        assertFalse(context.accepted());
        assertTrue(context.rejectionReason().orElseThrow().contains("No active registered guinea pig"));
    }

    @Test
    void thresholdHandlerUsesDefaultBaselineAndMarksAnomalousBehavior() {
        var guineaPig = guineaPig();
        BaselineProfileRepository profiles = _ -> Optional.empty();
        var handler = new BehaviorThresholdHandler(profiles);
        var context = new EventHandlerContext(
                behaviorEvent(UUID.randomUUID(), NOW, 0.8, 0.9)
        );
        context.identify(guineaPig);

        handler.handle(context);

        assertEquals(guineaPig.getId(), context.baselineProfile().orElseThrow().guineaPigId());
        assertTrue(context.anomalyReason().isPresent());
    }

    @Test
    void sustainedAnomalyRequiresTheConfiguredNumberOfConsecutiveWindows() {
        var guineaPig = guineaPig();
        var previousEvents = List.of(
                behaviorEvent(UUID.randomUUID(), NOW.minusSeconds(60), 0.8, 0.9),
                behaviorEvent(UUID.randomUUID(), NOW.minusSeconds(120), 0.8, 0.9)
        );
        var repository = eventRepository(previousEvents, new AtomicInteger());
        var context = anomalousContext(guineaPig);

        new SustainedAnomalyHandler(repository, 3).handle(context);

        assertEquals("Anomaly in 3 consecutive windows", context.anomalyReason().orElseThrow());
    }

    @Test
    void sustainedAnomalyClearsCandidateWhenAnEarlierWindowWasNormal() {
        var guineaPig = guineaPig();
        var previousEvents = List.of(
                event(
                        UUID.randomUUID(),
                        NOW.minusSeconds(60),
                        new BehaviorSignal(MarkColor.RED, 60, 10, 1, 0, 0.2, 0.1, 0.9)
                ),
                behaviorEvent(UUID.randomUUID(), NOW.minusSeconds(120), 0.8, 0.9)
        );
        var context = anomalousContext(guineaPig);

        new SustainedAnomalyHandler(eventRepository(previousEvents, new AtomicInteger()), 3)
                .handle(context);

        assertTrue(context.anomalyReason().isEmpty());
    }

    @Test
    void sustainedAnomalySkipsCageLevelEvents() {
        var context = new EventHandlerContext(
                event(UUID.randomUUID(), NOW, new AudioSignal(AudioLabel.NORMAL, 0.2, 960))
        );
        var queries = new AtomicInteger();

        new SustainedAnomalyHandler(eventRepository(List.of(), queries), 3).handle(context);

        assertTrue(context.accepted());
        assertEquals(0, queries.get());
    }

    private static EventHandlerContext anomalousContext(GuineaPig guineaPig) {
        var context = new EventHandlerContext(
                behaviorEvent(UUID.randomUUID(), NOW, 0.8, 0.9)
        );
        context.identify(guineaPig);
        context.useBaselineProfile(BaselineProfile.defaultFor(guineaPig.getId()));
        context.markAnomaly("classifier detected an anomaly");
        return context;
    }

    private static GuineaPig guineaPig() {
        return GuineaPig.restore(
                1L,
                "cage-1",
                "Cuy",
                MarkColor.RED,
                HealthStatus.NORMAL,
                NOW,
                true,
                NOW
        );
    }

    private static HealthEvent behaviorEvent(UUID eventId, Instant occurredAt, double probability, double confidence) {
        return event(
                eventId,
                occurredAt,
                new BehaviorSignal(MarkColor.RED, 60, 48, 0, 1, 0.72, probability, confidence)
        );
    }

    private static HealthEvent event(UUID eventId, Instant occurredAt, com.cuymonitor.backend.domain.model.HealthSignal signal) {
        return new HealthEvent(eventId, "cage-1", occurredAt, "test", signal);
    }

    private static GuineaPigRepository guineaPigRepository(
            Optional<GuineaPig> result,
            AtomicInteger lookups
    ) {
        return new GuineaPigRepository() {
            @Override
            public GuineaPig save(GuineaPig guineaPig) {
                return guineaPig;
            }

            @Override
            public Optional<GuineaPig> findById(long id) {
                return Optional.empty();
            }

            @Override
            public Optional<GuineaPig> findActiveByCageAndColor(String cageCode, MarkColor markColor) {
                lookups.incrementAndGet();
                assertEquals("cage-1", cageCode);
                assertEquals(MarkColor.RED, markColor);
                return result;
            }

            @Override
            public List<GuineaPig> findActiveByCage(String cageCode) {
                return List.of();
            }
        };
    }

    private static EventRepository eventRepository(List<HealthEvent> results, AtomicInteger queries) {
        return new EventRepository() {
            @Override
            public boolean existsById(UUID eventId) {
                return false;
            }

            @Override
            public void save(HealthEvent event, Long guineaPigId) {
            }

            @Override
            public List<HealthEvent> findLatestBehavior(long guineaPigId, int limit) {
                queries.incrementAndGet();
                assertEquals(1L, guineaPigId);
                assertEquals(3, limit);
                return results;
            }

            @Override
            public Optional<HealthEvent> findLatest(String cageCode, EventType type) {
                return Optional.empty();
            }
        };
    }
}
