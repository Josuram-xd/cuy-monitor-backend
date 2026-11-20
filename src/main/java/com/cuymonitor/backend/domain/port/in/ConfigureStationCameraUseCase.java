package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.CameraStatus;
import com.cuymonitor.backend.domain.model.StationCamera;

import java.time.Instant;
import java.util.Optional;

/** Sets up the camera of the demonstration kit from the dashboard, and hands it to the edge agent. */
public interface ConfigureStationCameraUseCase {

    /** What the breeder typed. A blank password keeps the one already stored. */
    record CameraSettings(String type, String host, Integer port, String username, String password, String stream) {
    }

    /** What the dashboard shows. Never contains the password, only whether there is one. */
    record CameraView(String type, String host, Integer port, String username, String stream, boolean hasPassword,
                      CameraStatus status, Instant statusAt) {
    }

    CameraView view(String cageCode);

    void save(String cageCode, CameraSettings settings);

    /** For the edge agent: the camera with its plain password, or empty when none is configured. */
    Optional<StationCamera> forAgent();

    void report(int version, CameraStatus status);
}
