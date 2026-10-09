package com.cuymonitor.backend.domain.health.chain;

import com.cuymonitor.backend.domain.health.fake.Events;
import com.cuymonitor.backend.domain.health.fake.HealthFakes;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.MarkColor;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class HandlerChainBuilderTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:00:00Z");

    private final HealthFakes.GuineaPigs guineaPigs = new HealthFakes.GuineaPigs();
    private final EventHandler chain = new HandlerChainBuilder(Clock.fixed(NOW, ZoneOffset.UTC), guineaPigs,
            new HealthFakes.Baselines(), new HealthFakes.Events(),
            new ChainSettings(Duration.ofHours(1), 0.5, 3)).build();

    @Test
    void firstHandlerIsValidation() {
        assertThat(chain).isInstanceOf(ValidationHandler.class);
    }

    @Test
    void aValidWindowGoesThroughEveryHandler() {
        guineaPigs.save(GuineaPig.register("cage-1", "Canela", MarkColor.RED, NOW));
        EventContext context = new EventContext(Events.anomalousWindow(MarkColor.RED, NOW));

        chain.handle(context);

        assertThat(context.guineaPig()).isPresent();
        assertThat(context.baseline()).isPresent();
        assertThat(context.isAnomalous()).isTrue();
    }

    @Test
    void aDroppedEventStopsTheChain() {
        guineaPigs.save(GuineaPig.register("cage-1", "Canela", MarkColor.RED, NOW));
        EventContext context = new EventContext(Events.behavior(MarkColor.RED, NOW, 55, 0, 0.2, 0.1, 0.1));

        chain.handle(context);

        assertThat(context.isDropped()).isTrue();
        assertThat(context.guineaPig()).isEmpty();
    }
}
