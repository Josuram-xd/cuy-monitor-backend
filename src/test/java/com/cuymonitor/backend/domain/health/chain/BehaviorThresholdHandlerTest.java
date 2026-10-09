package com.cuymonitor.backend.domain.health.chain;

import com.cuymonitor.backend.domain.health.fake.Events;
import com.cuymonitor.backend.domain.health.fake.HealthFakes;
import com.cuymonitor.backend.domain.model.BaselineProfile;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.MarkColor;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class BehaviorThresholdHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:00:00Z");
    private static final GuineaPig CANELA = GuineaPig.restore(1L, "cage-1", "Canela", MarkColor.RED,
            HealthStatus.NORMAL, NOW, true, NOW);

    private final HealthFakes.Baselines baselines = new HealthFakes.Baselines();
    private final BehaviorThresholdHandler handler = new BehaviorThresholdHandler(baselines);

    @Test
    void marksAWindowFarFromTheBaselineAsAnomalous() {
        EventContext context = identified(Events.anomalousWindow(MarkColor.RED, NOW));

        handler.handle(context);

        assertThat(context.isAnomalous()).isTrue();
        assertThat(context.anomalyReason()).contains("quieto");
    }

    @Test
    void leavesANormalWindowAlone() {
        EventContext context = identified(Events.normalWindow(MarkColor.RED, NOW));

        handler.handle(context);

        assertThat(context.isAnomalous()).isFalse();
        assertThat(context.baseline()).isPresent();
    }

    @Test
    void usesTheStoredBaselineWhenThereIsOne() {
        // this guinea pig is normally very calm, so 55 s still is not strange for it
        baselines.put(new BaselineProfile(1L, 50, 0, 0.3, NOW));
        EventContext context = identified(Events.anomalousWindow(MarkColor.RED, NOW));

        handler.handle(context);

        assertThat(context.isAnomalous()).isFalse();
    }

    @Test
    void skipsEventsWithoutAGuineaPig() {
        EventContext context = new EventContext(Events.weight(800, NOW));

        handler.handle(context);

        assertThat(context.baseline()).isEmpty();
    }

    private static EventContext identified(HealthEvent event) {
        EventContext context = new EventContext(event);
        context.identify(CANELA);
        return context;
    }
}
