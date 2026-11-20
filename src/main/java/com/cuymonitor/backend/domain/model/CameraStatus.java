package com.cuymonitor.backend.domain.model;

import java.util.EnumSet;
import java.util.Set;

/** State of the station camera. The edge agent reports the four in REPORTABLE; the backend works out the rest. */
public enum CameraStatus {
    PENDING,
    CONNECTED,
    AUTH_FAILED,
    UNREACHABLE,
    FFMPEG_MISSING,
    OFFLINE,
    NOT_CONFIGURED;

    public static final Set<CameraStatus> REPORTABLE =
            EnumSet.of(CONNECTED, AUTH_FAILED, UNREACHABLE, FFMPEG_MISSING);
}
