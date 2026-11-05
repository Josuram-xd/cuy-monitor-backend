package com.cuymonitor.backend.application;

import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryBaselineProfileRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryEventRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryGuineaPigRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryStateTransitionRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryWeightReadingRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryCageRepository;
import com.cuymonitor.backend.application.fake.Photos;
import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.exception.GuineaPigNotFoundException;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.GuineaPigHistory;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.StateTransition;
import com.cuymonitor.backend.domain.model.TimeRange;
import com.cuymonitor.backend.domain.model.WeightReading;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** History of a guinea pig and weight of a cage, against in-memory ports. */
class GuineaPigHistoryServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-04T12:00:00Z");
    private static final TimeRange LAST_DAY = TimeRange.of(null, NOW, NOW);

    private InMemoryEventRepository events;
    private InMemoryStateTransitionRepository transitions;
    private GuineaPigHistoryService history;
    private long pig;

    @BeforeEach
    void setUp() {
        var guineaPigs = new InMemoryGuineaPigRepository();
        events = new InMemoryEventRepository();
        transitions = new InMemoryStateTransitionRepository();
        history = new GuineaPigHistoryService(guineaPigs, transitions, events, new InMemoryBaselineProfileRepository());
        pig = guineaPigs.save(GuineaPig.register("cage-1", "RED", Photos.png(), NOW.minus(Duration.ofDays(5)))).getId();
    }

    @Test
    void keepsOnlyWhatHappenedInTheRangeOldestFirst() {
        window(NOW.minus(Duration.ofHours(1)), 40);
        window(NOW.minus(Duration.ofHours(3)), 20);
        window(NOW.minus(Duration.ofDays(2)), 55);
        transitions.save(new StateTransition(null, pig, HealthStatus.NORMAL, HealthStatus.OBSERVED, "x",
                NOW.minus(Duration.ofHours(2))));
        transitions.save(new StateTransition(null, pig, HealthStatus.OBSERVED, HealthStatus.NORMAL, "y",
                NOW.minus(Duration.ofDays(3))));

        GuineaPigHistory result = history.history(pig, LAST_DAY);

        assertThat(result.windows()).extracting(GuineaPigHistory.BehaviorWindow::stillSeconds)
                .containsExactly(20.0, 40.0);
        assertThat(result.transitions()).singleElement()
                .extracting(StateTransition::toStatus).isEqualTo(HealthStatus.OBSERVED);
    }

    @Test
    void usesTheDefaultBaselineUntilOneIsLearned() {
        assertThat(history.history(pig, LAST_DAY).baseline().avgStillSeconds()).isEqualTo(30.0);
    }

    @Test
    void anUnknownGuineaPigIsNotFound() {
        assertThatThrownBy(() -> history.history(999L, LAST_DAY)).isInstanceOf(GuineaPigNotFoundException.class);
    }

    @Test
    void theWeightOfACageKeepsStableAndUnstableReadingsOfTheRange() {
        var weights = new InMemoryWeightReadingRepository();
        var service = new CageWeightService(new InMemoryCageRepository(), weights);
        weights.save(new WeightReading(null, "cage-1", 3200, true, NOW.minus(Duration.ofHours(2))));
        weights.save(new WeightReading(null, "cage-1", 3400, false, NOW.minus(Duration.ofHours(1))));
        weights.save(new WeightReading(null, "cage-1", 3100, true, NOW.minus(Duration.ofDays(4))));

        assertThat(service.weight("cage-1", LAST_DAY).readings()).extracting(WeightReading::grams)
                .containsExactly(3200.0, 3400.0);
        assertThatThrownBy(() -> service.weight("cage-nope", LAST_DAY)).isInstanceOf(CageNotFoundException.class);
    }

    private void window(Instant at, double stillSeconds) {
        events.save(new HealthEvent(UUID.randomUUID(), "cage-1", at, "test",
                new BehaviorSignal("RED", 60, stillSeconds, 1, 1, 0.2, 0.1, 0.9)), pig);
    }
}
