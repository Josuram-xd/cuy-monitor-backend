package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.exception.MarkColorAlreadyUsedException;
import com.cuymonitor.backend.domain.health.fake.HealthFakes;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.MarkColor;
import com.cuymonitor.backend.domain.port.in.RegisterGuineaPigCommand;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuineaPigServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:10:00Z");

    private final HealthFakes.GuineaPigs guineaPigs = new HealthFakes.GuineaPigs();
    private final GuineaPigService service = new GuineaPigService(new HealthFakes.Cages(), guineaPigs,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void registersANormalGuineaPig() {
        GuineaPig pig = service.register(new RegisterGuineaPigCommand("cage-1", "Canela", MarkColor.RED));

        assertThat(pig.getId()).isNotNull();
        assertThat(pig.getStatus()).isEqualTo(HealthStatus.NORMAL);
        assertThat(pig.getCreatedAt()).isEqualTo(NOW);
        assertThat(service.listGuineaPigs("cage-1")).hasSize(1);
    }

    @Test
    void aColorCanOnlyBeUsedOncePerCage() {
        service.register(new RegisterGuineaPigCommand("cage-1", "Canela", MarkColor.RED));

        assertThatThrownBy(() -> service.register(new RegisterGuineaPigCommand("cage-1", "Otra", MarkColor.RED)))
                .isInstanceOf(MarkColorAlreadyUsedException.class);
    }

    @Test
    void unknownCageFails() {
        assertThatThrownBy(() -> service.register(new RegisterGuineaPigCommand("cage-9", "Canela", MarkColor.RED)))
                .isInstanceOf(CageNotFoundException.class);
        assertThatThrownBy(() -> service.listGuineaPigs("cage-9")).isInstanceOf(CageNotFoundException.class);
    }
}
