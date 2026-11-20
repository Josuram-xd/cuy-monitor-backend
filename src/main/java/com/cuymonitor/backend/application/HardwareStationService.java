package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.port.in.ConnectHardwareStationUseCase;
import com.cuymonitor.backend.domain.port.out.HardwareStation;

import java.util.Objects;

public final class HardwareStationService implements ConnectHardwareStationUseCase {

    private final HardwareStation station;

    public HardwareStationService(HardwareStation station) {
        this.station = Objects.requireNonNull(station, "station");
    }

    @Override
    public boolean connect(String cageCode) {
        return station.connect(cageCode);
    }

    @Override
    public void disconnect(String cageCode) {
        station.disconnect(cageCode);
    }

    @Override
    public boolean connectedTo(String cageCode) {
        return station.connectedTo(cageCode);
    }
}
