package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.HealthStatus;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthStateTest {

    @ParameterizedTest
    @MethodSource("states")
    void eachStateRepresentsItsHealthStatus(HealthState state, HealthStatus expectedStatus) {
        assertEquals(expectedStatus, state.status());
    }

    private static Stream<Arguments> states() {
        return Stream.of(
                Arguments.of(new NormalState(), HealthStatus.NORMAL),
                Arguments.of(new ObservedState(), HealthStatus.OBSERVED),
                Arguments.of(new AlertState(), HealthStatus.ALERT),
                Arguments.of(new CriticalState(), HealthStatus.CRITICAL)
        );
    }
}
