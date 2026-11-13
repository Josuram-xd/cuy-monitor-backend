package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.exception.CageAccessDeniedException;
import com.cuymonitor.backend.domain.exception.SimulationUnavailableException;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.port.out.SimulationRunner;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class SimulationServiceTest {

    private static final UUID OWNER = UUID.fromString("3f2a4b5c-0000-4000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-11-13T12:00:00Z");

    private final CageOperations cages = mock(CageOperations.class);
    private final SimulationRunner runner = mock(SimulationRunner.class);
    private final SimulationService service = new SimulationService(cages, runner);

    private static GuineaPig pig(long id, String color) {
        return GuineaPig.restore(id, "cage-1", "image/png", color, HealthStatus.NORMAL, NOW, true, NOW);
    }

    @Test
    void drawsTheMarksOfTheCageWithTheFirstOneSick() {
        given(cages.listGuineaPigs(OWNER, "cage-1")).willReturn(List.of(pig(1, "RED"), pig(2, "BLUE"),
                pig(3, "RED")));

        service.start(OWNER, "cage-1");

        verify(runner).start("cage-1", List.of("RED", "BLUE"));
    }

    @Test
    void aCageWithoutGuineaPigsHasNothingToDraw() {
        given(cages.listGuineaPigs(OWNER, "cage-1")).willReturn(List.of());

        assertThatThrownBy(() -> service.start(OWNER, "cage-1"))
                .isInstanceOfSatisfying(SimulationUnavailableException.class,
                        ex -> assertThat(ex.isNoGuineaPigs()).isTrue());
        verifyNoInteractions(runner);
    }

    @Test
    void onlyTheOwnerCanStartIt() {
        given(cages.listGuineaPigs(OWNER, "cage-9")).willThrow(new CageAccessDeniedException());

        assertThatThrownBy(() -> service.start(OWNER, "cage-9")).isInstanceOf(CageAccessDeniedException.class);
        verifyNoInteractions(runner);
    }
}
