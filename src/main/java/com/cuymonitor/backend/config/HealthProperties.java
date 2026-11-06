package com.cuymonitor.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.cuymonitor.backend.domain.health.weight.WeightTrendPolicy;
import com.cuymonitor.backend.domain.model.AnomalyThresholds;

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
 * @param classifierThreshold        from this probAnomaly on, the AI service's verdict is enough (Task 11.4)
 * @param stillFactor                still this many times longer than usual, without going to the feeder
 * @param groupDistanceFactor        this many times farther from the group than usual
 * @param baselineMinSamples         a learned profile is trusted over the default from this many windows on
 * @param baselineWarmupSamples      plain average up to this many windows, then a slow moving average
 * @param weightDropRatio            the cage is getting lighter from this drop on (0.1 = 10 %), Task 12.2
 * @param weightRecentWindow         the stable readings compared ...
 * @param weightBaselineWindow       ... against the ones of this window just before
 * @param weightMinReadings          stable readings needed in each window to say anything
 */
@ConfigurationProperties(prefix = "app.health")
public record HealthProperties(Duration maxEventAge, double minDetectionConfidence, int consecutiveAnomalyWindows,
                               double distressProbability, Duration observedToAlert, Duration alertToCritical,
                               int recoveryWindows, double classifierThreshold, double stillFactor,
                               double groupDistanceFactor, int baselineMinSamples, int baselineWarmupSamples,
                               double weightDropRatio, Duration weightRecentWindow, Duration weightBaselineWindow,
                               int weightMinReadings) {

    public AnomalyThresholds anomalyThresholds() {
        return new AnomalyThresholds(classifierThreshold, stillFactor, groupDistanceFactor);
    }

    public WeightTrendPolicy weightTrendPolicy() {
        return new WeightTrendPolicy(weightDropRatio, weightRecentWindow, weightBaselineWindow, weightMinReadings);
    }
}
