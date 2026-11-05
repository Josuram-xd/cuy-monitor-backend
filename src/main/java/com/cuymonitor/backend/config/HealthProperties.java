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
 * @param observedToAlert            how long a confirmed anomaly has to last in OBSERVED to become ALERT
 * @param alertToCritical            how long it has to last in ALERT to become CRITICAL
 * @param recoveryWindows            how many normal windows in a row bring a guinea pig back to NORMAL
 */
@ConfigurationProperties(prefix = "app.health")
public record HealthProperties(Duration maxEventAge, double minDetectionConfidence, int consecutiveAnomalyWindows,
                               double distressProbability, Duration observedToAlert, Duration alertToCritical,
                               int recoveryWindows) {
}
