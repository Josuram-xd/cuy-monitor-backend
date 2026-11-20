package com.cuymonitor.backend.config;

import com.cuymonitor.backend.application.AssistantService;
import com.cuymonitor.backend.application.CageWeightEvaluator;
import com.cuymonitor.backend.application.ConfigurableEventProcessor;
import com.cuymonitor.backend.application.SettingsService;
import com.cuymonitor.backend.domain.model.AlertMessages;
import com.cuymonitor.backend.domain.model.settings.SettingKey;
import com.cuymonitor.backend.domain.port.out.SettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * "Reglas y mensajes": the defaults come from application.yml (and the texts from the code), the admin's changes from
 * the table app_setting, and the result goes to the health engine and the assistant.
 */
@Configuration
public class SettingsConfig {

    private static final Logger log = LoggerFactory.getLogger(SettingsConfig.class);

    @Bean
    public Map<SettingKey, String> settingDefaults(HealthProperties health, AssistantProperties assistant) {
        Map<SettingKey, String> defaults = new EnumMap<>(health.asDefaults());
        defaults.put(SettingKey.MESSAGE_ALERT, AlertMessages.DEFAULTS.alert());
        defaults.put(SettingKey.MESSAGE_CRITICAL, AlertMessages.DEFAULTS.critical());
        defaults.put(SettingKey.MESSAGE_DISTRESS, AlertMessages.DEFAULTS.distress());
        defaults.put(SettingKey.MESSAGE_WEIGHT_DROP, CageWeightEvaluator.DEFAULT_DROP_MESSAGE);
        defaults.put(SettingKey.ASSISTANT_MAX_QUESTIONS, String.valueOf(assistant.maxQuestions()));
        defaults.put(SettingKey.ASSISTANT_WINDOW_MINUTES, String.valueOf(assistant.questionWindow().toMinutes()));
        return defaults;
    }

    @Bean
    public SettingsService settingsService(Map<SettingKey, String> settingDefaults, SettingRepository repository,
                                           ConfigurableEventProcessor eventProcessor,
                                           AssistantService assistantService,
                                           CurrentAnomalyThresholds anomalyThresholds) {
        // every listener of the settings: the health engine, the assistant's limit, and the thresholds the live
        // symptoms and the alert notes read
        SettingsService service = new SettingsService(settingDefaults, repository,
                List.of(eventProcessor, assistantService, anomalyThresholds));
        try {
            service.applyStored();
        } catch (DataAccessException e) {
            // the table is not there yet (V18 not applied): the defaults keep working, nothing is lost
            log.warn("Could not read the admin's settings, using the defaults: {}", e.getMessage());
        }
        return service;
    }
}
