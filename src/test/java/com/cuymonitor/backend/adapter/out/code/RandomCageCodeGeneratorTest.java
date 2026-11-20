package com.cuymonitor.backend.adapter.out.code;

import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryCageRepository;
import com.cuymonitor.backend.domain.model.Cage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RandomCageCodeGeneratorTest {

    private final InMemoryCageRepository cages = new InMemoryCageRepository();

    @Test
    void aFarmCodeIsCagePlusEightHex() {
        assertThat(new RandomCageCodeGenerator(cages).newCode()).matches("cage-[0-9a-f]{8}");
    }

    @Test
    void aSimulationCodeIsSimPlusEightHex() {
        assertThat(new RandomCageCodeGenerator(cages, Cage.SIMULATION_PREFIX).newCode())
                .matches("sim-[0-9a-f]{8}");
    }
}
