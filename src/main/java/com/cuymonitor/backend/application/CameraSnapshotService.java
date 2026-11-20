package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.model.Cage;
import com.cuymonitor.backend.domain.port.in.GetLatestCameraFrameUseCase;
import com.cuymonitor.backend.domain.port.out.HardwareStation;
import com.cuymonitor.backend.domain.port.out.LatestFrameSource;

import java.util.Objects;
import java.util.Optional;

/**
 * Which camera a cage shows. The simulation cage shows its own video frames. The cage the kit is connected to shows
 * the kit's camera, which the edge agent sends under the kit's own cage id. The owner check is the caller's job.
 */
public class CameraSnapshotService implements GetLatestCameraFrameUseCase {

    private final HardwareStation station;
    private final LatestFrameSource frames;

    public CameraSnapshotService(HardwareStation station, LatestFrameSource frames) {
        this.station = Objects.requireNonNull(station, "station");
        this.frames = Objects.requireNonNull(frames, "frames");
    }

    @Override
    public Optional<byte[]> latest(String cageCode) {
        if (Cage.isSimulationCode(cageCode)) {
            return frames.latest(cageCode);
        }
        if (station.connectedTo(cageCode)) {
            return frames.latest(HardwareStation.DEVICE_CODE);
        }
        return Optional.empty();
    }
}
