package com.cuymonitor.backend.domain.port.in;

/** Binds the one physical demonstration kit to a cage, or frees it. */
public interface ConnectHardwareStationUseCase {

    /** @return false when the kit is already bound to another cage */
    boolean connect(String cageCode);

    void disconnect(String cageCode);

    /** true when the kit is connected to this cage right now: the cage is "monitored" by the real equipment. */
    boolean connectedTo(String cageCode);
}
