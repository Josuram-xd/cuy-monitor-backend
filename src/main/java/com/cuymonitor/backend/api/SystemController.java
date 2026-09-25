package com.cuymonitor.backend.api;


import com.cuymonitor.backend.config.KafkaConfigTopics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final String apiKey;

    public SystemController(KafkaTemplate<String, String> kafkaTemplate, JdbcTemplate jdbcTemplate,
                            @Value("${app.api-key}") String apiKey) {
        this.kafkaTemplate = kafkaTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.apiKey = apiKey;
    }

    //check DB status returning total cages
    @GetMapping("/status")
    public Map<String, Object> status() {
        Integer cages = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cage", Integer.class);
        return Map.of(
                "status", "Up",
                "cages", cages
        );
    }

    //Publish a text message to Kafka
    @PostMapping("/ping")
    public ResponseEntity<?> ping(@RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        if (!this.apiKey.equals(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid API key"));
        }
        String id = UUID.randomUUID().toString();

        kafkaTemplate.send(KafkaConfigTopics.EVENTS_CAMERA, id, "{\"type\":\"ping\",\"id\":\"" + id + "\"}");

        return ResponseEntity.ok(Map.of("sent", id));
    }




}
