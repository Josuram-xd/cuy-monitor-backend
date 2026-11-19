package com.cuymonitor.backend.domain.model.settings;

import java.math.BigDecimal;

/** One setting as the admin screen shows it: its value now, the default it goes back to, and its range. */
public record SettingView(String key, SettingKey.Kind kind, String value, String defaultValue, BigDecimal min,
                          BigDecimal max) {
}
