package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.adapter.in.ingestion.dto.IngestionEvent;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single entry point for data coming from outside: ai-service and the Arduino serial bridge.
 */
@RestController
@RequestMapping("/api/v1/ingestion")
public class IngestionController {

    private static final Logger log = LoggerFactory.getLogger(IngestionController.class);

    private final byte[] apiKey;

    public IngestionController(@Value("${app.api-key}") String apiKey) {
        this.apiKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping("/events")
    public ResponseEntity<Map<String, Object>> receive(
            @RequestHeader(value = "X-API-Key", required = false) String providedKey,
            @Valid @RequestBody IngestionEvent event) {

        if (providedKey == null
                || !MessageDigest.isEqual(apiKey, providedKey.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiError.body(ApiError.UNAUTHORIZED, "invalid api key"));
        }

        log.info("Received event id={} type={} cage={} source={}",
                event.eventId(), event.type(), event.cageId(), event.source());

        // TODO (Task 5.5): AdapterFactory -> EventSourceAdapter -> HealthEvent -> ProcessEventUseCase

        return ResponseEntity.accepted().body(Map.of("eventId", event.eventId(), "status", "ACCEPTED"));
    }
}
