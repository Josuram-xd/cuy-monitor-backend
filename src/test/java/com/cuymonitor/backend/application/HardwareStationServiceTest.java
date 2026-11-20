package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.port.out.HardwareStation;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HardwareStationServiceTest {

    private static final class FakeStation implements HardwareStation {
        String bound;

        public Optional<Binding> current() {
            return Optional.ofNullable(bound).map(code -> new Binding(code, Instant.EPOCH));
        }

        public boolean connect(String cageCode) {
            if (bound != null && !bound.equals(cageCode)) return false;
            bound = cageCode;
            return true;
        }

        public void disconnect(String cageCode) {
            if (cageCode.equals(bound)) bound = null;
        }
    }

    @Test
    void delegatesToTheStation() {
        var station = new FakeStation();
        var service = new HardwareStationService(station);

        assertTrue(service.connect("A"));
        assertFalse(service.connect("B"));
        service.disconnect("A");
        assertTrue(service.connect("B"));
    }
}
