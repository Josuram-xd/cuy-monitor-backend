package com.cuymonitor.backend.domain.health.chain;

import com.cuymonitor.backend.domain.model.BaselineProfile;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.MarkColor;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandlerChainBuilderTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:32:00Z");

    @Test
    void buildRunsValidationIdentificationAndThresholdBeforeSustainedAnomaly() {
        var guineaPigRepository = new RecordingGuineaPigRepository();
        var baselineRepository = new RecordingBaselineProfileRepository();
        var eventRepository = new RecordingEventRepository();
        var chain = chain(guineaPigRepository, baselineRepository, eventRepository, 3);
        var context = new EventHandlerContext(event(
                new BehaviorSignal(MarkColor.RED, 60, 10, 1, 0, 0.2, 0.1, 0.9)
        ));

        chain.handle(context);

        assertTrue(context.accepted());
        assertEquals(1L, context.guineaPig().orElseThrow().getId());
        assertTrue(context.baselineProfile().isPresent());
        assertTrue(context.anomalyReason().isEmpty());
        assertEquals(1, guineaPigRepository.lookups.get());
        assertEquals(1, baselineRepository.lookups.get());
        assertEquals(0, eventRepository.lookups.get());
    }

    @Test
    void buildStopsBeforeIdentificationWhenValidationRejectsAnEvent() {
        var guineaPigRepository = new RecordingGuineaPigRepository();
        var baselineRepository = new RecordingBaselineProfileRepository();
        var eventRepository = new RecordingEventRepository();
        var chain = chain(guineaPigRepository, baselineRepository, eventRepository, 3);
        var context = new EventHandlerContext(
                event(new BehaviorSignal(MarkColor.RED, 60, 10, 1, 0, 0.2, 0.1, 0.2))
        );

        chain.handle(context);

        assertFalse(context.accepted());
        assertEquals(0, guineaPigRepository.lookups.get());
        assertEquals(0, baselineRepository.lookups.get());
        assertEquals(0, eventRepository.lookups.get());
    }

    @Test
    void buildConfirmsAnomalyAfterThresholdEvaluationWhenOneWindowIsConfigured() {
        var guineaPigRepository = new RecordingGuineaPigRepository();
        var baselineRepository = new RecordingBaselineProfileRepository();
        var eventRepository = new RecordingEventRepository();
        var chain = chain(guineaPigRepository, baselineRepository, eventRepository, 1);
        var context = new EventHandlerContext(
                event(new BehaviorSignal(MarkColor.RED, 60, 48, 0, 1, 0.72, 0.8, 0.9))
        );

        chain.handle(context);

        assertEquals("Anomaly in 1 consecutive window", context.anomalyReason().orElseThrow());
        assertEquals(1, guineaPigRepository.lookups.get());
        assertEquals(1, baselineRepository.lookups.get());
    }

    private static EventHandler chain(
            GuineaPigRepository guineaPigRepository,
            BaselineProfileRepository baselineProfileRepository,
            EventRepository eventRepository,
            int requiredConsecutiveWindows
    ) {
        return new HandlerChainBuilder(
                new ValidationHandler(
                        Clock.fixed(NOW, ZoneOffset.UTC),
                        Duration.ofMinutes(5),
                        0.5
                ),
                new IdentificationHandler(guineaPigRepository),
                new BehaviorThresholdHandler(baselineProfileRepository),
                new SustainedAnomalyHandler(eventRepository, requiredConsecutiveWindows)
        ).build();
    }

    private static HealthEvent event(BehaviorSignal signal) {
        return new HealthEvent(
                UUID.randomUUID(),
                "cage-1",
                NOW,
                "test",
                signal
        );
    }

    private static final class RecordingGuineaPigRepository implements GuineaPigRepository {

        private final AtomicInteger lookups = new AtomicInteger();
        private final GuineaPig guineaPig = GuineaPig.restore(
                1L,
                "cage-1",
                "Cuy",
                MarkColor.RED,
                HealthStatus.NORMAL,
                NOW,
                true,
                NOW
        );

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
            return Optional.of(guineaPig);
        }

        @Override
        public List<GuineaPig> findActiveByCage(String cageCode) {
            return List.of(guineaPig);
        }
    }

    private static final class RecordingBaselineProfileRepository implements BaselineProfileRepository {

        private final AtomicInteger lookups = new AtomicInteger();

        @Override
        public Optional<BaselineProfile> findByGuineaPigId(long guineaPigId) {
            lookups.incrementAndGet();
            return Optional.of(BaselineProfile.defaultFor(guineaPigId));
        }
    }

    private static final class RecordingEventRepository implements EventRepository {

        private final AtomicInteger lookups = new AtomicInteger();

        @Override
        public boolean existsById(UUID eventId) {
            return false;
        }

        @Override
        public void save(HealthEvent event, Long guineaPigId) {
        }

        @Override
        public List<HealthEvent> findLatestBehavior(long guineaPigId, int limit) {
            lookups.incrementAndGet();
            return List.of();
        }

        @Override
        public Optional<HealthEvent> findLatest(String cageCode, EventType type) {
            return Optional.empty();
        }
    }
}
