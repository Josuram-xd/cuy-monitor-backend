package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.adapter.in.web.dto.CameraStatusReport;
import com.cuymonitor.backend.domain.model.StationCamera;
import com.cuymonitor.backend.domain.port.in.ConfigureStationCameraUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/** The camera as the edge agent reads it (with the password, inside the URL) and reports it. Same key as ingestion. */
@RestController
@RequestMapping("/api/v1/ingestion/station/camera")
public class StationCameraAgentController {
    private final byte[] apiKey;
    private final ConfigureStationCameraUseCase camera;

    public StationCameraAgentController(@Value("${app.api-key}") String apiKey, ConfigureStationCameraUseCase camera) {
        this.apiKey = apiKey.getBytes(StandardCharsets.UTF_8);
        this.camera = camera;
    }

    /** 204 when no camera is configured, so the agent falls back to its .env. */
    @GetMapping
    public ResponseEntity<Map<String, Object>> current(
            @RequestHeader(value = "X-API-Key", required = false) String providedKey) {
        if (!authorized(providedKey)) {
            return unauthorized();
        }
        var stored = camera.forAgent();
        if (stored.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        StationCamera current = stored.get();
        return ResponseEntity.ok(Map.<String, Object>of(
                "version", current.version(), "type", current.type(), "url", current.url()));
    }

    @PostMapping("/status")
    public ResponseEntity<Map<String, Object>> report(
            @RequestHeader(value = "X-API-Key", required = false) String providedKey,
            @RequestBody CameraStatusReport report) {
        if (!authorized(providedKey)) {
            return unauthorized();
        }
        camera.report(report.version(), report.status());
        return ResponseEntity.noContent().build();
    }

    private boolean authorized(String providedKey) {
        return providedKey != null
                && MessageDigest.isEqual(apiKey, providedKey.getBytes(StandardCharsets.UTF_8));
    }

    private static ResponseEntity<Map<String, Object>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.body(ApiError.UNAUTHORIZED, "invalid api key"));
    }
}
