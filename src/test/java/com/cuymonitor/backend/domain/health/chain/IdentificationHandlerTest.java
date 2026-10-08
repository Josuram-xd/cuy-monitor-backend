package com.cuymonitor.backend.domain.health.chain;

import com.cuymonitor.backend.domain.health.fake.Events;
import com.cuymonitor.backend.domain.health.fake.HealthFakes;
import com.cuymonitor.backend.domain.model.AudioLabel;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.MarkColor;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class IdentificationHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:00:00Z");

    private final HealthFakes.GuineaPigs guineaPigs = new HealthFakes.GuineaPigs();
    private final IdentificationHandler handler = new IdentificationHandler(guineaPigs);

    @Test
    void mapsTheColorToTheRegisteredGuineaPig() {
        GuineaPig canela = guineaPigs.save(GuineaPig.register("cage-1", "Canela", MarkColor.RED, NOW));
        EventContext context = new EventContext(Events.normalWindow(MarkColor.RED, NOW));

        handler.handle(context);

        assertThat(context.guineaPig()).get().extracting(GuineaPig::getId).isEqualTo(canela.getId());
    }

    @Test
    void dropsWindowsOfAnUnknownColor() {
        guineaPigs.save(GuineaPig.register("cage-1", "Canela", MarkColor.RED, NOW));
        EventContext context = new EventContext(Events.normalWindow(MarkColor.BLUE, NOW));

        handler.handle(context);

        assertThat(context.dropReason()).contains("BLUE");
    }

    @Test
    void cageLevelEventsAreNotTiedToAGuineaPig() {
        EventContext context = new EventContext(Events.audio(AudioLabel.DISTRESS, 0.9, NOW));

        handler.handle(context);

        assertThat(context.isDropped()).isFalse();
        assertThat(context.guineaPig()).isEmpty();
    }
}
