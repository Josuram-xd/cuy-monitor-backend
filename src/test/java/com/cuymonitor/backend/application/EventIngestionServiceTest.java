package com.cuymonitor.backend.application;

import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryCageRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryEventRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryGuineaPigRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryWeightReadingRepository;
import com.cuymonitor.backend.domain.model.AudioLabel;
import com.cuymonitor.backend.domain.model.AudioSignal;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.HealthSignal;
import com.cuymonitor.backend.domain.model.MarkColor;
import com.cuymonitor.backend.domain.model.WeightSignal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventIngestionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:32:00Z");
    private static final String CAGE = "cage-1";

    private InMemoryGuineaPigRepository guineaPigs;
    private InMemoryEventRepository events;
    private InMemoryWeightReadingRepository weights;
    private EventIngestionService service;

    @BeforeEach
    void setUp() {
        guineaPigs = new InMemoryGuineaPigRepository();
        events = new InMemoryEventRepository();
        weights = new InMemoryWeightReadingRepository();
        service = new EventIngestionService(new InMemoryCageRepository(), guineaPigs, events, weights);
        guineaPigs.save(GuineaPig.register(CAGE, "Canela", MarkColor.RED, NOW.minusSeconds(3600)));
    }

    @Test
    void storesAWeightEventAndItsReading() {
        service.process(event(new WeightSignal(812.4, true)));

        assertThat(events.findLatest(CAGE, EventType.WEIGHT)).isPresent();
        assertThat(weights.findLatest(CAGE).orElseThrow().grams()).isEqualTo(812.4);
        assertThat(weights.findLatest(CAGE).orElseThrow().measuredAt()).isEqualTo(NOW);
    }

    @Test
    void storesAnAudioEventWithoutAGuineaPig() {
        service.process(event(new AudioSignal(AudioLabel.DISTRESS, 0.9, 1500)));

        assertThat(events.findLatest(CAGE, EventType.AUDIO)).isPresent();
    }

    @Test
    void attachesABehaviorWindowToTheGuineaPigWearingThatColor() {
        service.process(event(new BehaviorSignal(MarkColor.RED, 60, 48, 1, 2, 0.4, 0.2, 0.9)));

        assertThat(events.findLatestBehavior(1, 10)).hasSize(1);
    }

    @Test
    void ignoresABehaviorWindowOfAColorNobodyWears() {
        service.process(event(new BehaviorSignal(MarkColor.BLUE, 60, 48, 1, 2, 0.4, 0.2, 0.9)));

        assertThat(events.findLatestBehavior(1, 10)).isEmpty();
        assertThat(events.findLatest(CAGE, EventType.BEHAVIOR)).isEmpty();
    }

    @Test
    void aRetriedEventIsStoredOnlyOnce() {
        HealthEvent weight = event(new WeightSignal(500, true));

        service.process(weight);
        service.process(weight);

        assertThat(weights.findLatest(CAGE)).isPresent();
        assertThat(events.existsById(weight.eventId())).isTrue();
    }

    @Test
    void anUnknownCageIsRefused() {
        HealthEvent elsewhere = new HealthEvent(UUID.randomUUID(), "nope", NOW, "arduino", new WeightSignal(1, true));

        assertThatThrownBy(() -> service.process(elsewhere)).isInstanceOf(IllegalArgumentException.class);
    }

    private static HealthEvent event(HealthSignal signal) {
        return new HealthEvent(UUID.randomUUID(), CAGE, NOW, "test", signal);
    }
}
