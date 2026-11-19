package com.cuymonitor.backend.domain.model.settings;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * The value of every setting right now: the defaults with the admin's changes on top. Immutable; a change makes a
 * new one, which is what the health engine and the assistant are rebuilt from.
 */
public final class Settings {

    private final Map<SettingKey, String> values;

    private Settings(Map<SettingKey, String> values) {
        this.values = values;
    }

    /**
     * The defaults with the stored changes on top. A stored value that is unknown or out of range (a key removed in
     * a later version, a range made narrower) is skipped, so a bad row can never stop the backend from starting.
     */
    public static Settings resolve(Map<SettingKey, String> defaults, Map<String, String> stored) {
        Map<SettingKey, String> values = new EnumMap<>(SettingKey.class);
        for (SettingKey key : SettingKey.values()) {
            values.put(key, Objects.requireNonNull(defaults.get(key), "no default for " + key.key()));
        }
        stored.forEach((key, value) -> SettingKey.of(key).ifPresent(k -> {
            try {
                values.put(k, k.normalize(value));
            } catch (IllegalArgumentException ignored) {
                // keeps the default
            }
        }));
        return new Settings(values);
    }

    public String text(SettingKey key) {
        return values.get(key);
    }

    public int integer(SettingKey key) {
        return Integer.parseInt(values.get(key));
    }

    public double decimal(SettingKey key) {
        return Double.parseDouble(values.get(key));
    }

    public Duration minutes(SettingKey key) {
        return Duration.ofMinutes(integer(key));
    }

    public Map<SettingKey, String> asMap() {
        return Map.copyOf(values);
    }
}
