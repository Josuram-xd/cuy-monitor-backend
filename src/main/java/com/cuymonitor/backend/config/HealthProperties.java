package com.cuymonitor.backend.config;

import java.util.Map;
import java.util.EnumMap;
import java.math.BigDecimal;
import com.cuymonitor.backend.domain.model.settings.Settings;
import com.cuymonitor.backend.domain.model.settings.SettingKey;
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

    /** The rules an admin set ("Reglas y mensajes"), in the shape the health engine is built from. */
    public static HealthProperties from(Settings s) {
        return new HealthProperties(s.minutes(SettingKey.HEALTH_MAX_EVENT_AGE_MINUTES),
                s.decimal(SettingKey.HEALTH_MIN_DETECTION_CONFIDENCE),
                s.integer(SettingKey.HEALTH_CONSECUTIVE_ANOMALY_WINDOWS),
                s.decimal(SettingKey.HEALTH_DISTRESS_PROBABILITY),
                s.minutes(SettingKey.HEALTH_OBSERVED_TO_ALERT_MINUTES),
                s.minutes(SettingKey.HEALTH_ALERT_TO_CRITICAL_MINUTES),
                s.integer(SettingKey.HEALTH_RECOVERY_WINDOWS),
                s.decimal(SettingKey.HEALTH_CLASSIFIER_THRESHOLD),
                s.decimal(SettingKey.HEALTH_STILL_FACTOR),
                s.decimal(SettingKey.HEALTH_GROUP_DISTANCE_FACTOR),
                s.integer(SettingKey.HEALTH_BASELINE_MIN_SAMPLES),
                s.integer(SettingKey.HEALTH_BASELINE_WARMUP_SAMPLES),
                s.integer(SettingKey.HEALTH_WEIGHT_DROP_PERCENT) / 100.0,
                Duration.ofHours(s.integer(SettingKey.HEALTH_WEIGHT_RECENT_HOURS)),
                Duration.ofDays(s.integer(SettingKey.HEALTH_WEIGHT_BASELINE_DAYS)),
                s.integer(SettingKey.HEALTH_WEIGHT_MIN_READINGS));
    }

    /** These properties as the defaults of the settings: what "volver a los valores de fábrica" goes back to. */
    public Map<SettingKey, String> asDefaults() {
        Map<SettingKey, String> d = new EnumMap<>(SettingKey.class);
        d.put(SettingKey.HEALTH_MAX_EVENT_AGE_MINUTES, String.valueOf(maxEventAge.toMinutes()));
        d.put(SettingKey.HEALTH_MIN_DETECTION_CONFIDENCE, plain(minDetectionConfidence));
        d.put(SettingKey.HEALTH_CONSECUTIVE_ANOMALY_WINDOWS, String.valueOf(consecutiveAnomalyWindows));
        d.put(SettingKey.HEALTH_DISTRESS_PROBABILITY, plain(distressProbability));
        d.put(SettingKey.HEALTH_OBSERVED_TO_ALERT_MINUTES, String.valueOf(observedToAlert.toMinutes()));
        d.put(SettingKey.HEALTH_ALERT_TO_CRITICAL_MINUTES, String.valueOf(alertToCritical.toMinutes()));
        d.put(SettingKey.HEALTH_RECOVERY_WINDOWS, String.valueOf(recoveryWindows));
        d.put(SettingKey.HEALTH_CLASSIFIER_THRESHOLD, plain(classifierThreshold));
        d.put(SettingKey.HEALTH_STILL_FACTOR, plain(stillFactor));
        d.put(SettingKey.HEALTH_GROUP_DISTANCE_FACTOR, plain(groupDistanceFactor));
        d.put(SettingKey.HEALTH_BASELINE_MIN_SAMPLES, String.valueOf(baselineMinSamples));
        d.put(SettingKey.HEALTH_BASELINE_WARMUP_SAMPLES, String.valueOf(baselineWarmupSamples));
        d.put(SettingKey.HEALTH_WEIGHT_DROP_PERCENT, String.valueOf(Math.round(weightDropRatio * 100)));
        d.put(SettingKey.HEALTH_WEIGHT_RECENT_HOURS, String.valueOf(weightRecentWindow.toHours()));
        d.put(SettingKey.HEALTH_WEIGHT_BASELINE_DAYS, String.valueOf(weightBaselineWindow.toDays()));
        d.put(SettingKey.HEALTH_WEIGHT_MIN_READINGS, String.valueOf(weightMinReadings));
        return d;
    }

    private static String plain(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
