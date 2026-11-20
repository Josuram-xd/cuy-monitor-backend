package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.HealthStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class HealthStatesTest {

    @Test
    void returnsTheSameInstanceForEveryStatus() {
        for (HealthStatus status : HealthStatus.values()) {
            assertThat(HealthStates.of(status)).isSameAs(HealthStates.of(status));
        }
    }

    @Test
    void transitionsReuseTheSharedStates() {
        assertThat(HealthStates.of(HealthStatus.NORMAL).onAnomaly(Duration.ZERO, EscalationPolicy.immediate()))
                .isSameAs(HealthStates.of(HealthStatus.OBSERVED));
        assertThat(HealthStates.of(HealthStatus.ALERT).onRecovery())
                .isSameAs(HealthStates.of(HealthStatus.NORMAL));
    }
}
