package com.cuymonitor.backend.domain.port.out;

import java.time.Instant;
import java.util.Optional;

/** The one physical demonstration kit. A binding survives logout and server restarts. */
public interface HardwareStation {
    String DEVICE_CODE = "farm-station";
    record Binding(String cageCode, Instant since) {}
    Optional<Binding> current();
    boolean connect(String cageCode);
    void disconnect(String cageCode);
    default boolean connectedTo(String cageCode) {
        return current().filter(binding -> binding.cageCode().equals(cageCode)).isPresent();
    }
}
