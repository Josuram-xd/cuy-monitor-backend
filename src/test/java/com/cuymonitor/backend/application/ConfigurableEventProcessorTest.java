package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.settings.SettingKey;
import com.cuymonitor.backend.domain.model.settings.Settings;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ConfigurableEventProcessorTest {

    @Test
    void theNextEventUsesTheEngineBuiltFromTheNewRules() {
        List<String> handledWith = new ArrayList<>();
        ConfigurableEventProcessor processor = new ConfigurableEventProcessor(
                settings -> event -> handledWith.add(settings.text(SettingKey.MESSAGE_ALERT)),
                Settings.resolve(SettingsServiceTest.defaults(), Map.of()));
        HealthEvent event = mock(HealthEvent.class);

        processor.process(event);
        Map<SettingKey, String> changed = new EnumMap<>(SettingsServiceTest.defaults());
        changed.put(SettingKey.MESSAGE_ALERT, "Revisa a tu cuy");
        processor.onSettings(Settings.resolve(changed, Map.of()));
        processor.process(event);

        assertThat(handledWith).containsExactly("texto de message.alert", "Revisa a tu cuy");
    }
}
