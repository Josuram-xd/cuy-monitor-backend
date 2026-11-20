package com.cuymonitor.backend.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CageTest {

    @Test
    void aCodeWithTheSimulationPrefixMakesASimulationCage() {
        assertThat(new Cage(1L, "sim-0a1b2c3d", "Simulación").simulated()).isTrue();
        assertThat(Cage.isSimulationCode("sim-0a1b2c3d")).isTrue();
    }

    @Test
    void aFarmCageIsNotASimulationCage() {
        assertThat(new Cage(1L, "cage-1", "Jaula piloto").simulated()).isFalse();
        assertThat(Cage.isSimulationCode("cage-0a1b2c3d")).isFalse();
    }
}
