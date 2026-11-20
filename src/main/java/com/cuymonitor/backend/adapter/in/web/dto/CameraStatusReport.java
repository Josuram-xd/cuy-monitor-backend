package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.CameraStatus;

/** What the edge agent tells the backend after trying the camera. version is the one it read. */
public record CameraStatusReport(int version, CameraStatus status) {
}
