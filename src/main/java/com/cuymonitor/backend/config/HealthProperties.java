package com.cuymonitor.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * The thresholds of the health evaluation (application.yml, app.health). Starting values; they are tuned with
 * real data from the cage.
 *
 * @param maxEventAge                an event older than this is stored but not evaluated
 * @param minDetectionConfidence     a behavior window detected with less confidence is not evaluated
 * @param consecutiveAnomalyWindows  how many anomalous windows in a row confirm an anomaly
 * @param distressProbability        how sure the audio classifier must be of distress to alert
 */
@ConfigurationProperties(prefix = "app.health")
public record HealthProperties(Duration maxEventAge, double minDetectionConfidence, int consecutiveAnomalyWindows,
                               double distressProbability) {
}
