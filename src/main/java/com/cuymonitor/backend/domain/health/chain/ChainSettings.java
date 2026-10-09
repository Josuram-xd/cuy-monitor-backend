package com.cuymonitor.backend.domain.health.chain;

import java.time.Duration;
import java.util.Objects;

public record ChainSettings(Duration maxEventAge, double minDetectionConfidence, int sustainedWindows) {

    public ChainSettings {
        Objects.requireNonNull(maxEventAge, "maxEventAge");
        if (minDetectionConfidence < 0 || minDetectionConfidence > 1) {
            throw new IllegalArgumentException("minDetectionConfidence must be between 0 and 1");
        }
        if (sustainedWindows < 1) {
            throw new IllegalArgumentException("sustainedWindows must be at least 1");
        }
    }
}
