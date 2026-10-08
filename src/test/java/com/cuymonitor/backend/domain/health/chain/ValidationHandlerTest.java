package com.cuymonitor.backend.domain.health.chain;

import com.cuymonitor.backend.domain.health.fake.Events;
import com.cuymonitor.backend.domain.model.AudioLabel;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.MarkColor;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:00:00Z");

    private final ValidationHandler handler =
            new ValidationHandler(Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofHours(1), 0.5);

    @Test
    void recentConfidentWindowPasses() {
        EventContext context = run(Events.normalWindow(MarkColor.RED, NOW.minusSeconds(30)));

        assertThat(context.isDropped()).isFalse();
    }

    @Test
    void dropsEventsFromTheFuture() {
        EventContext context = run(Events.normalWindow(MarkColor.RED, NOW.plus(Duration.ofMinutes(10))));

        assertThat(context.dropReason()).contains("future");
    }

    @Test
    void acceptsSmallClockDifferencesWithTheProducer() {
        assertThat(run(Events.normalWindow(MarkColor.RED, NOW.plus(Duration.ofMinutes(2)))).isDropped()).isFalse();
    }

    @Test
    void dropsStaleEvents() {
        EventContext context = run(Events.normalWindow(MarkColor.RED, NOW.minus(Duration.ofHours(2))));

        assertThat(context.dropReason()).contains("stale");
    }

    @Test
    void dropsLowConfidenceDetections() {
        EventContext context = run(Events.behavior(MarkColor.RED, NOW, 10, 2, 0.2, 0.1, 0.3));

        assertThat(context.dropReason()).contains("confidence");
    }

    @Test
    void confidenceOnlyAppliesToBehavior() {
        assertThat(run(Events.audio(AudioLabel.DISTRESS, 0.3, NOW)).isDropped()).isFalse();
    }

    private EventContext run(HealthEvent event) {
        EventContext context = new EventContext(event);
        handler.handle(context);
        return context;
    }
}
