package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.health.composite.CageHealth;
import com.cuymonitor.backend.domain.health.fake.Events;
import com.cuymonitor.backend.domain.health.fake.HealthFakes;
import com.cuymonitor.backend.domain.model.AudioLabel;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.MarkColor;
import com.cuymonitor.backend.domain.model.WeightReading;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CageQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:32:05Z");

    private final HealthFakes.GuineaPigs guineaPigs = new HealthFakes.GuineaPigs();
    private final HealthFakes.Events events = new HealthFakes.Events();
    private final HealthFakes.Weights weights = new HealthFakes.Weights();
    private final CageQueryService service = new CageQueryService(new HealthFakes.Cages(), guineaPigs, events,
            weights, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void buildsTheCageFromItsGuineaPigsAudioAndWeight() {
        GuineaPig canela = GuineaPig.register("cage-1", "Canela", MarkColor.RED, NOW);
        canela.changeStatus(HealthStatus.OBSERVED, NOW);
        guineaPigs.save(canela);
        events.save(Events.audio(AudioLabel.NORMAL, 0.9, NOW.minusSeconds(60)), null);
        weights.save(new WeightReading(null, "cage-1", 812.4, true, NOW.minusSeconds(5)));

        CageHealth cage = service.getCageHealth("cage-1");

        assertThat(cage.status()).isEqualTo(HealthStatus.OBSERVED);
        assertThat(cage.guineaPigs()).hasSize(1);
        assertThat(cage.audio().orElseThrow().lastEventAt()).contains(NOW.minusSeconds(60));
        assertThat(cage.weight().orElseThrow().lastReading()).get().extracting(WeightReading::grams).isEqualTo(812.4);
    }

    @Test
    void unknownCageFails() {
        assertThatThrownBy(() -> service.getCageHealth("cage-9")).isInstanceOf(CageNotFoundException.class);
    }
}
