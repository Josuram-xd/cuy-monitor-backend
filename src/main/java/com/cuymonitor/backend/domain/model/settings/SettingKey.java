package com.cuymonitor.backend.domain.model.settings;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/**
 * Everything an admin may change from "Reglas y mensajes", each with its type and the range a value must stay in.
 * The range is the safety net: a typo can not make an alert take a week or fire on every window.
 * For TEXT, min and max are the length.
 */
public enum SettingKey {

    HEALTH_MAX_EVENT_AGE_MINUTES("health.maxEventAgeMinutes", Kind.INTEGER, 1, 60),
    HEALTH_MIN_DETECTION_CONFIDENCE("health.minDetectionConfidence", Kind.DECIMAL, 0, 1),
    HEALTH_CONSECUTIVE_ANOMALY_WINDOWS("health.consecutiveAnomalyWindows", Kind.INTEGER, 1, 20),
    HEALTH_DISTRESS_PROBABILITY("health.distressProbability", Kind.DECIMAL, 0.5, 1),
    HEALTH_OBSERVED_TO_ALERT_MINUTES("health.observedToAlertMinutes", Kind.INTEGER, 1, 240),
    HEALTH_ALERT_TO_CRITICAL_MINUTES("health.alertToCriticalMinutes", Kind.INTEGER, 1, 480),
    HEALTH_RECOVERY_WINDOWS("health.recoveryWindows", Kind.INTEGER, 1, 30),
    HEALTH_CLASSIFIER_THRESHOLD("health.classifierThreshold", Kind.DECIMAL, 0.1, 1),
    HEALTH_STILL_FACTOR("health.stillFactor", Kind.DECIMAL, 1.05, 5),
    HEALTH_GROUP_DISTANCE_FACTOR("health.groupDistanceFactor", Kind.DECIMAL, 1.05, 5),
    HEALTH_BASELINE_MIN_SAMPLES("health.baselineMinSamples", Kind.INTEGER, 1, 500),
    HEALTH_BASELINE_WARMUP_SAMPLES("health.baselineWarmupSamples", Kind.INTEGER, 1, 1000),
    HEALTH_WEIGHT_DROP_PERCENT("health.weightDropPercent", Kind.INTEGER, 1, 50),
    HEALTH_WEIGHT_RECENT_HOURS("health.weightRecentHours", Kind.INTEGER, 1, 72),
    HEALTH_WEIGHT_BASELINE_DAYS("health.weightBaselineDays", Kind.INTEGER, 1, 30),
    HEALTH_WEIGHT_MIN_READINGS("health.weightMinReadings", Kind.INTEGER, 1, 100),
    MESSAGE_ALERT("message.alert", Kind.TEXT, 1, 500),
    MESSAGE_CRITICAL("message.critical", Kind.TEXT, 1, 500),
    MESSAGE_DISTRESS("message.distress", Kind.TEXT, 1, 500),
    // "{porcentaje}" is replaced by how much the cage got lighter
    MESSAGE_WEIGHT_DROP("message.weightDrop", Kind.TEXT, 1, 500),
    // 0 = no limit, which is the default
    ASSISTANT_MAX_QUESTIONS("assistant.maxQuestions", Kind.INTEGER, 0, 1000),
    ASSISTANT_WINDOW_MINUTES("assistant.windowMinutes", Kind.INTEGER, 1, 1440);

    public enum Kind { INTEGER, DECIMAL, TEXT }

    private final String key;
    private final Kind kind;
    private final BigDecimal min;
    private final BigDecimal max;

    SettingKey(String key, Kind kind, double min, double max) {
        this.key = key;
        this.kind = kind;
        this.min = BigDecimal.valueOf(min);
        this.max = BigDecimal.valueOf(max);
    }

    public String key() {
        return key;
    }

    public Kind kind() {
        return kind;
    }

    public BigDecimal min() {
        return min;
    }

    public BigDecimal max() {
        return max;
    }

    public static Optional<SettingKey> of(String key) {
        return Arrays.stream(values()).filter(k -> k.key.equals(key)).findFirst();
    }

    /** The value as it is stored, or an IllegalArgumentException saying what is wrong with it. */
    public String normalize(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException(key + " needs a value");
        }
        if (kind == Kind.TEXT) {
            String text = raw.strip();
            if (text.length() < min.intValue() || text.length() > max.intValue()) {
                throw new IllegalArgumentException(key + " must have between " + min.intValue() + " and "
                        + max.intValue() + " characters");
            }
            return text;
        }
        BigDecimal number;
        try {
            number = new BigDecimal(raw.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be a number");
        }
        if (kind == Kind.INTEGER && number.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(key + " must be a whole number");
        }
        if (number.compareTo(min) < 0 || number.compareTo(max) > 0) {
            throw new IllegalArgumentException(key + " must be between " + min.stripTrailingZeros().toPlainString()
                    + " and " + max.stripTrailingZeros().toPlainString());
        }
        return number.stripTrailingZeros().toPlainString();
    }
}
