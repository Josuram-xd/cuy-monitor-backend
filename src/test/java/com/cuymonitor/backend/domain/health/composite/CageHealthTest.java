package com.cuymonitor.backend.domain.health.composite;

import com.cuymonitor.backend.domain.health.state.AlertState;
import com.cuymonitor.backend.domain.health.state.GuineaPigHealthContext;
import com.cuymonitor.backend.domain.health.state.NormalState;
import com.cuymonitor.backend.domain.model.HealthStatus;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CageHealthTest {

    @Test
    void reportsTheWorstStatusAcrossItsComponents() {
        var cageHealth = new CageHealth(List.of(
                new GuineaPigHealth(new GuineaPigHealthContext(1L, new AlertState())),
                new CageAudioHealth(HealthStatus.NORMAL),
                new CageWeightHealth(HealthStatus.CRITICAL)
        ));

        assertEquals(HealthStatus.CRITICAL, cageHealth.status());
    }

    @Test
    void reportsNormalWhenItHasNoComponents() {
        assertEquals(HealthStatus.NORMAL, new CageHealth(List.of()).status());
    }

    @Test
    void rejectsNullComponents() {
        assertThrows(NullPointerException.class, () -> new CageHealth(null));
        assertThrows(NullPointerException.class, () -> new CageHealth(Arrays.asList(
                new CageAudioHealth(HealthStatus.NORMAL), null
        )));
    }

    @Test
    void leavesExposeTheirHealthStatus() {
        var guineaPig = new GuineaPigHealth(new GuineaPigHealthContext(1L, new NormalState()));

        assertEquals(HealthStatus.NORMAL, guineaPig.status());
        assertEquals(HealthStatus.ALERT, new CageAudioHealth(HealthStatus.ALERT).status());
        assertEquals(HealthStatus.OBSERVED, new CageWeightHealth(HealthStatus.OBSERVED).status());
    }
}
