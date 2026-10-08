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

class SustainedAnomalyHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:00:00Z");
    private static final GuineaPig CANELA = GuineaPig.restore(1L, "cage-1", "Canela", MarkColor.RED,
            HealthStatus.NORMAL, NOW, true, NOW);

    private final HealthFakes.Events events = new HealthFakes.Events();
    private final SustainedAnomalyHandler handler = new SustainedAnomalyHandler(events, 3);

    @Test
    void confirmsAnAnomalySeenInThreeWindowsInARow() {
        storePrevious(Events.anomalousWindow(MarkColor.RED, NOW.minusSeconds(120)),
                Events.anomalousWindow(MarkColor.RED, NOW.minusSeconds(60)));

        EventContext context = evaluate(Events.anomalousWindow(MarkColor.RED, NOW), true);

        assertThat(context.isSustainedAnomaly()).isTrue();
        assertThat(context.isSustainedRecovery()).isFalse();
    }

    @Test
    void oneNormalWindowInBetweenIsNotSustained() {
        storePrevious(Events.anomalousWindow(MarkColor.RED, NOW.minusSeconds(120)),
                Events.normalWindow(MarkColor.RED, NOW.minusSeconds(60)));

        EventContext context = evaluate(Events.anomalousWindow(MarkColor.RED, NOW), true);

        assertThat(context.isSustainedAnomaly()).isFalse();
    }

    @Test
    void notEnoughHistoryConfirmsNothing() {
        storePrevious(Events.anomalousWindow(MarkColor.RED, NOW.minusSeconds(60)));

        EventContext context = evaluate(Events.anomalousWindow(MarkColor.RED, NOW), true);

        assertThat(context.isSustainedAnomaly()).isFalse();
        assertThat(context.isSustainedRecovery()).isFalse();
    }

    @Test
    void confirmsRecoveryAfterThreeNormalWindows() {
        storePrevious(Events.normalWindow(MarkColor.RED, NOW.minusSeconds(120)),
                Events.normalWindow(MarkColor.RED, NOW.minusSeconds(60)));

        EventContext context = evaluate(Events.normalWindow(MarkColor.RED, NOW), false);

        assertThat(context.isSustainedRecovery()).isTrue();
    }

    @Test
    void windowsArrivingLaterThanTheCurrentOneAreIgnored() {
        storePrevious(Events.anomalousWindow(MarkColor.RED, NOW.minusSeconds(60)),
                Events.anomalousWindow(MarkColor.RED, NOW.plusSeconds(60)));

        EventContext context = evaluate(Events.anomalousWindow(MarkColor.RED, NOW), true);

        assertThat(context.isSustainedAnomaly()).isFalse();
    }

    private void storePrevious(HealthEvent... previous) {
        for (HealthEvent event : previous) {
            events.save(event, CANELA.getId());
        }
    }

    private EventContext evaluate(HealthEvent current, boolean anomalous) {
        EventContext context = new EventContext(current);
        context.identify(CANELA);
        context.useBaseline(BaselineProfile.defaultFor(CANELA.getId()));
        if (anomalous) {
            context.markAnomalous("estuvo quieto");
        }
        handler.handle(context);
        return context;
    }
}
