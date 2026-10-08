package com.cuymonitor.backend.domain.health.composite;

import com.cuymonitor.backend.domain.health.fake.Events;
import com.cuymonitor.backend.domain.model.AudioLabel;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.MarkColor;
import com.cuymonitor.backend.domain.model.WeightReading;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CageHealthTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:00:00Z");

    @Test
    void emptyCageIsNormal() {
        assertThat(new CageHealth("cage-1").status()).isEqualTo(HealthStatus.NORMAL);
    }

    @Test
    void cageTakesTheWorstStatusOfItsGuineaPigs() {
        CageHealth cage = CageHealth.of("cage-1",
                List.of(pig(1, HealthStatus.NORMAL), pig(2, HealthStatus.ALERT), pig(3, HealthStatus.OBSERVED)),
                Optional.empty(), Optional.empty(), NOW);

        assertThat(cage.status()).isEqualTo(HealthStatus.ALERT);
        assertThat(cage.guineaPigs()).hasSize(3);
    }

    @Test
    void recentDistressCallRaisesTheCageToAlert() {
        CageHealth cage = CageHealth.of("cage-1", List.of(pig(1, HealthStatus.NORMAL)),
                Optional.of(Events.audio(AudioLabel.DISTRESS, 0.9, NOW.minusSeconds(30))), Optional.empty(), NOW);

        assertThat(cage.status()).isEqualTo(HealthStatus.ALERT);
        assertThat(cage.audio().orElseThrow().lastEventAt()).contains(NOW.minusSeconds(30));
    }

    @Test
    void oldOrUnclearDistressDoesNotCount() {
        CageAudioHealth old = new CageAudioHealth(
                Optional.of(Events.audio(AudioLabel.DISTRESS, 0.9, NOW.minus(Duration.ofMinutes(10)))), NOW);
        CageAudioHealth unclear = new CageAudioHealth(
                Optional.of(Events.audio(AudioLabel.DISTRESS, 0.5, NOW)), NOW);
        CageAudioHealth calm = new CageAudioHealth(Optional.of(Events.audio(AudioLabel.NORMAL, 0.99, NOW)), NOW);

        assertThat(old.status()).isEqualTo(HealthStatus.NORMAL);
        assertThat(unclear.status()).isEqualTo(HealthStatus.NORMAL);
        assertThat(calm.status()).isEqualTo(HealthStatus.NORMAL);
    }

    @Test
    void weightLeafKeepsTheLastReadingAndIsNormalForNow() {
        WeightReading reading = new WeightReading(1L, "cage-1", 812.4, true, NOW);
        CageHealth cage = CageHealth.of("cage-1", List.of(), Optional.empty(), Optional.of(reading), NOW);

        assertThat(cage.weight().orElseThrow().lastReading()).contains(reading);
        assertThat(cage.status()).isEqualTo(HealthStatus.NORMAL);
    }

    @Test
    void aCompositeCanContainAnotherComposite() {
        CageHealth inner = new CageHealth("cage-1");
        inner.add(new GuineaPigHealth(pig(1, HealthStatus.CRITICAL)));
        CageHealth outer = new CageHealth("farm");
        outer.add(inner);

        assertThat(outer.status()).isEqualTo(HealthStatus.CRITICAL);
    }

    private static GuineaPig pig(long id, HealthStatus status) {
        return GuineaPig.restore(id, "cage-1", "Cuy " + id, MarkColor.values()[(int) id], status, NOW, true, NOW);
    }
}
