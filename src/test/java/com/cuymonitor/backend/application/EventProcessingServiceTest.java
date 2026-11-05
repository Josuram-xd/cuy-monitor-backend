package com.cuymonitor.backend.application;

import com.cuymonitor.backend.adapter.out.notification.DatabaseAlertObserver;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryAlertRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryBaselineProfileRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryCageRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryEventRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryGuineaPigRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryWeightReadingRepository;
import com.cuymonitor.backend.application.fake.MutableClock;
import com.cuymonitor.backend.application.fake.Photos;
import com.cuymonitor.backend.domain.health.chain.BehaviorThresholdHandler;
import com.cuymonitor.backend.domain.health.chain.HandlerChainBuilder;
import com.cuymonitor.backend.domain.health.chain.IdentificationHandler;
import com.cuymonitor.backend.domain.health.chain.SustainedAnomalyHandler;
import com.cuymonitor.backend.domain.health.chain.SustainedRecoveryHandler;
import com.cuymonitor.backend.domain.health.state.EscalationPolicy;
import com.cuymonitor.backend.domain.health.chain.ValidationHandler;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AudioLabel;
import com.cuymonitor.backend.domain.model.AudioSignal;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.HealthSignal;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.StateTransition;
import com.cuymonitor.backend.domain.notification.AlertPublisher;
import com.cuymonitor.backend.domain.port.out.StateTransitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The whole health core with plain JUnit: the real chain, the real states and in-memory ports. No Spring and
 * no database. Two anomalous windows in a row confirm an anomaly and two normal ones confirm a recovery.
 */
class EventProcessingServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");
    private static final String CAGE = "cage-1";

    private InMemoryGuineaPigRepository guineaPigs;
    private InMemoryEventRepository events;
    private InMemoryAlertRepository alerts;
    private final List<StateTransition> transitions = new ArrayList<>();
    // what the dashboard would receive: the observer after the database one
    private final List<Alert> pushed = new ArrayList<>();
    private EventProcessingService service;
    private GuineaPig canela;
    private int minute;

    @BeforeEach
    void setUp() {
        var clock = new MutableClock(NOW);
        guineaPigs = new InMemoryGuineaPigRepository();
        events = new InMemoryEventRepository();
        alerts = new InMemoryAlertRepository();
        var cages = new InMemoryCageRepository();
        var chain = new HandlerChainBuilder(
                new ValidationHandler(clock, Duration.ofMinutes(10), 0.5),
                new IdentificationHandler(guineaPigs),
                new BehaviorThresholdHandler(new InMemoryBaselineProfileRepository()),
                new SustainedAnomalyHandler(events, 2),
                new SustainedRecoveryHandler(events, 2)).build();
        var recorder = new EventIngestionService(cages, guineaPigs, events, new InMemoryWeightReadingRepository());
        StateTransitionRepository recordTransitions = new StateTransitionRepository() {
            @Override
            public StateTransition save(StateTransition transition) {
                transitions.add(transition);
                return transition;
            }

            @Override
            public List<StateTransition> findByGuineaPigBetween(long guineaPigId, Instant from, Instant to) {
                return List.of();
            }
        };
        var publisher = new AlertPublisher(List.of(new DatabaseAlertObserver(alerts), pushed::add));
        // no waiting between levels here: this test is about the flow; the times are in HealthRulesTest
        service = new EventProcessingService(recorder, chain, guineaPigs, recordTransitions, publisher, clock, 0.8,
                EscalationPolicy.immediate());
        canela = guineaPigs.save(GuineaPig.register(CAGE, "RED", Photos.png(), NOW.minus(Duration.ofDays(3))));
    }

    @Test
    void oneAnomalousWindowAloneChangesNothing() {
        service.process(window(0.9));

        assertThat(statusOfCanela()).isEqualTo(HealthStatus.NORMAL);
        assertThat(transitions).isEmpty();
        assertThat(alerts.findAll()).isEmpty();
    }

    @Test
    void aSustainedAnomalyClimbsOneLevelPerWindowAndAlertsFromAlertUp() {
        service.process(window(0.9));
        service.process(window(0.9));
        assertThat(statusOfCanela()).isEqualTo(HealthStatus.OBSERVED);
        assertThat(alerts.findAll()).isEmpty();

        service.process(window(0.9));
        assertThat(statusOfCanela()).isEqualTo(HealthStatus.ALERT);

        service.process(window(0.9));
        assertThat(statusOfCanela()).isEqualTo(HealthStatus.CRITICAL);

        assertThat(transitions).extracting(StateTransition::toStatus)
                .containsExactly(HealthStatus.OBSERVED, HealthStatus.ALERT, HealthStatus.CRITICAL);
        assertThat(alerts.findAll()).extracting(Alert::getLevel)
                .containsExactlyInAnyOrder(HealthStatus.ALERT, HealthStatus.CRITICAL);
        assertThat(alerts.findAll()).allSatisfy(alert -> {
            assertThat(alert.getGuineaPigId()).isEqualTo(canela.getId());
            assertThat(alert.getType()).isEqualTo(EventType.BEHAVIOR);
        });
    }

    @Test
    void twoNormalWindowsInARowBringItBackToNormalWithoutAnAlert() {
        for (int i = 0; i < 3; i++) {
            service.process(window(0.9));
        }
        int alertsBefore = alerts.findAll().size();

        service.process(window(0.1));
        assertThat(statusOfCanela()).as("one good window is not enough").isEqualTo(HealthStatus.ALERT);

        service.process(window(0.1));

        assertThat(statusOfCanela()).isEqualTo(HealthStatus.NORMAL);
        assertThat(transitions.getLast().fromStatus()).isEqualTo(HealthStatus.ALERT);
        assertThat(transitions.getLast().toStatus()).isEqualTo(HealthStatus.NORMAL);
        assertThat(alerts.findAll()).hasSize(alertsBefore);
    }

    @Test
    void aRetriedEventIsEvaluatedOnlyOnce() {
        service.process(window(0.9));
        HealthEvent second = event(behavior(0.9));

        service.process(second);
        service.process(second);

        assertThat(transitions).hasSize(1);
    }

    @Test
    void aWindowDetectedWithLowConfidenceIsStoredButNotEvaluated() {
        service.process(window(0.9));

        service.process(event(new BehaviorSignal("RED", 60, 20, 2, 1, 0.2, 0.9, 0.3)));

        assertThat(statusOfCanela()).isEqualTo(HealthStatus.NORMAL);
    }

    @Test
    void aConfidentDistressClipIsAnAlertOfTheWholeCage() {
        service.process(event(new AudioSignal(AudioLabel.DISTRESS, 0.92, 1200)));
        service.process(event(new AudioSignal(AudioLabel.DISTRESS, 0.4, 1200)));
        service.process(event(new AudioSignal(AudioLabel.NORMAL, 0.99, 1200)));

        assertThat(alerts.findAll()).singleElement().satisfies(alert -> {
            assertThat(alert.getType()).isEqualTo(EventType.AUDIO);
            assertThat(alert.getGuineaPigId()).isNull();
            assertThat(alert.getLevel()).isEqualTo(HealthStatus.ALERT);
        });
    }

    private HealthStatus statusOfCanela() {
        return guineaPigs.findById(canela.getId()).orElseThrow().getStatus();
    }

    private HealthEvent window(double probAnomaly) {
        return event(behavior(probAnomaly));
    }

    private static BehaviorSignal behavior(double probAnomaly) {
        // a calm window except for what the classifier says
        return new BehaviorSignal("RED", 60, 20, 2, 1, 0.2, probAnomaly, 0.95);
    }

    // every window a minute after the previous one, all within the last 10 minutes
    private HealthEvent event(HealthSignal signal) {
        return new HealthEvent(UUID.randomUUID(), CAGE, NOW.minus(Duration.ofMinutes(9)).plus(Duration.ofMinutes(minute++)),
                "test", signal);
    }

    @Test
    void everyAlertIsStoredFirstAndReachesTheDashboardWithItsId() {
        for (int i = 0; i < 3; i++) {
            service.process(window(0.9));
        }

        assertThat(pushed).singleElement().satisfies(alert -> {
            assertThat(alert.getId()).isNotNull();
            assertThat(alerts.findById(alert.getId())).isPresent();
            assertThat(alert.getLevel()).isEqualTo(HealthStatus.ALERT);
        });
    }

    @Test
    void aRecoveryIsNotPublished() {
        for (int i = 0; i < 3; i++) {
            service.process(window(0.9));
        }
        pushed.clear();

        service.process(window(0.1));
        service.process(window(0.1));

        assertThat(pushed).isEmpty();
    }
}
