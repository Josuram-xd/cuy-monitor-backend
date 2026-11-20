package com.cuymonitor.backend.domain.port.out;

import com.cuymonitor.backend.domain.model.CameraStatus;
import com.cuymonitor.backend.domain.model.StationCamera;

import java.util.Optional;

/** The camera of the kit. The adapter encrypts the password; this port only sees plain text. */
public interface StationCameraRepository {

    Optional<StationCamera> find();

    /** Replaces the camera, bumps its version and clears the last report. */
    void save(String type, String host, int port, String username, String password, String stream);

    /** @return false when the version is no longer the current one (a late report) */
    boolean report(int version, CameraStatus status);
}
