package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.model.settings.SettingKey;
import com.cuymonitor.backend.domain.model.settings.SettingView;
import com.cuymonitor.backend.domain.model.settings.Settings;
import com.cuymonitor.backend.domain.port.out.SettingRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettingsServiceTest {

    private static final UUID ADMIN = UUID.fromString("3f2a4b5c-0000-4000-8000-000000000001");

    private final Map<String, String> stored = new HashMap<>();
    private final SettingRepository repository = new SettingRepository() {
        @Override
        public Map<String, String> findAll() {
            return Map.copyOf(stored);
        }

        @Override
        public void replaceAll(Map<String, String> changes, UUID changedBy) {
            stored.clear();
            stored.putAll(changes);
        }
    };
    private final List<Settings> heard = new ArrayList<>();
    private final SettingsService service = new SettingsService(defaults(), repository, List.of(heard::add));

    static Map<SettingKey, String> defaults() {
        Map<SettingKey, String> d = new EnumMap<>(SettingKey.class);
        for (SettingKey key : SettingKey.values()) {
            d.put(key, switch (key.kind()) {
                case TEXT -> "texto de " + key.key();
                case DECIMAL -> "1.5".equals(key.min().toPlainString()) ? "2" : key.max().stripTrailingZeros()
                        .toPlainString();
                case INTEGER -> key.min().stripTrailingZeros().toPlainString();
            });
        }
        d.put(SettingKey.HEALTH_OBSERVED_TO_ALERT_MINUTES, "30");
        d.put(SettingKey.ASSISTANT_MAX_QUESTIONS, "0");
        return d;
    }

    private String value(List<SettingView> view, SettingKey key) {
        return view.stream().filter(v -> v.key().equals(key.key())).findFirst().orElseThrow().value();
    }

    @Test
    void appliesAChangeAtOnceAndStoresOnlyWhatDiffersFromTheDefaults() {
        List<SettingView> view = service.update(Map.of(
                "health.observedToAlertMinutes", "20",
                "message.alert", "  Revisa a tu cuy  ",
                "assistant.maxQuestions", "0"), ADMIN);

        assertThat(value(view, SettingKey.HEALTH_OBSERVED_TO_ALERT_MINUTES)).isEqualTo("20");
        assertThat(value(view, SettingKey.MESSAGE_ALERT)).isEqualTo("Revisa a tu cuy");
        // assistant.maxQuestions is already the default, so it is not stored
        assertThat(stored).containsOnlyKeys("health.observedToAlertMinutes", "message.alert");
        assertThat(heard.getLast().minutes(SettingKey.HEALTH_OBSERVED_TO_ALERT_MINUTES))
                .isEqualTo(Duration.ofMinutes(20));
    }

    @Test
    void oneValueOutOfRangeChangesNothing() {
        assertThatThrownBy(() -> service.update(Map.of(
                "health.observedToAlertMinutes", "20",
                "health.alertToCriticalMinutes", "100000"), ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("health.alertToCriticalMinutes must be between 1 and 480");

        assertThat(stored).isEmpty();
        assertThat(heard).isEmpty();
    }

    @Test
    void refusesUnknownKeysDecimalsWhereAWholeNumberGoesAndEmptyTexts() {
        assertThatThrownBy(() -> service.update(Map.of("health.nope", "1"), ADMIN))
                .hasMessageContaining("unknown setting");
        assertThatThrownBy(() -> service.update(Map.of("health.recoveryWindows", "2.5"), ADMIN))
                .hasMessageContaining("whole number");
        assertThatThrownBy(() -> service.update(Map.of("message.distress", "   "), ADMIN))
                .hasMessageContaining("between 1 and 500 characters");
        assertThatThrownBy(() -> service.update(Map.of("health.stillFactor", "abc"), ADMIN))
                .hasMessageContaining("must be a number");
    }

    @Test
    void goesBackToTheDefaults() {
        service.update(Map.of("health.observedToAlertMinutes", "20"), ADMIN);

        List<SettingView> view = service.reset(ADMIN);

        assertThat(stored).isEmpty();
        assertThat(value(view, SettingKey.HEALTH_OBSERVED_TO_ALERT_MINUTES)).isEqualTo("30");
    }

    @Test
    void readsTheStoredChangesAtStartupAndSkipsOnesThatNoLongerFit() {
        stored.put("health.observedToAlertMinutes", "45");
        stored.put("health.recoveryWindows", "9999");
        stored.put("health.removedLongAgo", "1");

        service.applyStored();

        Settings now = heard.getLast();
        assertThat(now.integer(SettingKey.HEALTH_OBSERVED_TO_ALERT_MINUTES)).isEqualTo(45);
        assertThat(now.text(SettingKey.HEALTH_RECOVERY_WINDOWS)).isEqualTo(defaults().get(SettingKey.HEALTH_RECOVERY_WINDOWS));
    }
}
