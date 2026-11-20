package com.cuymonitor.backend.config;

import com.cuymonitor.backend.application.AssistantService;
import com.cuymonitor.backend.application.ConfigurableEventProcessor;
import com.cuymonitor.backend.application.SettingsService;
import com.cuymonitor.backend.domain.model.AnomalyThresholds;
import com.cuymonitor.backend.domain.port.out.SettingRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SettingsConfigTest {

    private static final UUID ADMIN = UUID.fromString("3f2a4b5c-0000-4000-8000-000000000001");

    private final SettingsConfig config = new SettingsConfig();
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

    // the values application.yml ships with
    private static HealthProperties health() {
        return new HealthProperties(Duration.ofMinutes(10), 0.5, 3, 0.7, Duration.ofMinutes(30),
                Duration.ofMinutes(60), 3, 0.7, 1.5, 2.0, 5, 60, 0.1, Duration.ofHours(24), Duration.ofDays(7), 5);
    }

    private static AssistantProperties assistant() {
        return new AssistantProperties(false, "us-east-1", "model", 600, Duration.ofSeconds(20), 0,
                Duration.ofMinutes(10));
    }

    private SettingsService serviceWith(CurrentAnomalyThresholds thresholds) {
        return config.settingsService(config.settingDefaults(health(), assistant()), repository,
                mock(ConfigurableEventProcessor.class), mock(AssistantService.class), thresholds);
    }

    @Test
    void theLiveSymptomsAndAlertNotesFollowTheThresholdsAnAdminSaves() {
        CurrentAnomalyThresholds thresholds = new CurrentAnomalyThresholds();
        SettingsService settings = serviceWith(thresholds);

        settings.update(Map.of("health.stillFactor", "3", "health.groupDistanceFactor", "4"), ADMIN);

        assertThat(thresholds.get()).isEqualTo(new AnomalyThresholds(0.7, 3, 4));

        settings.reset(ADMIN);

        assertThat(thresholds.get()).isEqualTo(new AnomalyThresholds(0.7, 1.5, 2.0));
    }

    @Test
    void theThresholdsStoredBeforeAreTheOnesInUseAfterARestart() {
        stored.put("health.classifierThreshold", "0.9");
        CurrentAnomalyThresholds thresholds = new CurrentAnomalyThresholds();

        serviceWith(thresholds);

        assertThat(thresholds.get().classifierThreshold()).isEqualTo(0.9);
    }
}
