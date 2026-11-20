package com.cuymonitor.backend.application;

import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryCageRepository;
import com.cuymonitor.backend.application.fake.MutableClock;
import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.exception.SimulationUnavailableException;
import com.cuymonitor.backend.domain.model.Cage;
import com.cuymonitor.backend.domain.model.GuineaPigPhoto;
import com.cuymonitor.backend.domain.port.out.SimulationRunner;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SimulationCageServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");
    private static final UUID OWNER = UUID.fromString("3f2a4b5c-0000-4000-8000-000000000001");
    private static final UUID OTHER = UUID.fromString("3f2a4b5c-0000-4000-8000-000000000002");

    private final InMemoryCageRepository cages = new InMemoryCageRepository();
    private final CageOperations cageOperations = mock(CageOperations.class);
    private final SimulationRunner runner = mock(SimulationRunner.class);
    private final AtomicInteger codes = new AtomicInteger();
    private final SimulationCageService service = new SimulationCageService(cages,
            () -> "sim-" + String.format("%08x", codes.incrementAndGet()), runner, cageOperations,
            new MutableClock(NOW));

    @Test
    void createsTheSimulationCageWithAGuineaPigPerClipColor() {
        given(runner.clipColors()).willReturn(List.of("RED", "BLUE"));

        Cage cage = service.getOrCreate(OWNER);

        assertThat(cage.code()).isEqualTo("sim-00000001");
        assertThat(cage.name()).isEqualTo("Simulación");
        assertThat(cage.simulated()).isTrue();
        assertThat(cage.ownerId()).isEqualTo(OWNER);
        verify(cageOperations).registerGuineaPig(eq(OWNER), eq("sim-00000001"), eq("RED"),
                any(GuineaPigPhoto.class), isNull());
        verify(cageOperations).registerGuineaPig(eq(OWNER), eq("sim-00000001"), eq("BLUE"),
                any(GuineaPigPhoto.class), isNull());
    }

    @Test
    void theSecondCallGivesBackTheSameCageAndDoesNotAskAgain() {
        given(runner.clipColors()).willReturn(List.of("RED"));

        Cage first = service.getOrCreate(OWNER);
        Cage second = service.getOrCreate(OWNER);

        assertThat(second.code()).isEqualTo(first.code());
        verify(runner, times(1)).clipColors();
    }

    @Test
    void eachAccountHasItsOwnSimulationCage() {
        given(runner.clipColors()).willReturn(List.of("RED"));

        Cage mine = service.getOrCreate(OWNER);
        Cage theirs = service.getOrCreate(OTHER);

        assertThat(theirs.code()).isNotEqualTo(mine.code());
        assertThat(theirs.ownerId()).isEqualTo(OTHER);
    }

    @Test
    void anAiServiceWithoutClipLeavesNothingBehind() {
        given(runner.clipColors()).willThrow(SimulationUnavailableException.noClip());

        assertThatThrownBy(() -> service.getOrCreate(OWNER)).isInstanceOf(SimulationUnavailableException.class);
        assertThat(cages.findActiveByOwner(OWNER)).isEmpty();
        verify(cageOperations, never()).registerGuineaPig(any(), anyString(), anyString(), any(), any());
    }
}
