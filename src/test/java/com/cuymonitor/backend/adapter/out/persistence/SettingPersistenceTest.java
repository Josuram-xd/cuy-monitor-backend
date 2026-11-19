package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.integration.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// table app_setting (V18 of cuy-monitor-db), for real
class SettingPersistenceTest extends PostgresIntegrationTest {

    @Autowired
    private SettingPersistenceAdapter settings;

    @Test
    void keepsExactlyTheChangesItIsGivenAndForgetsThemOnReset() {
        settings.replaceAll(Map.of("health.observedToAlertMinutes", "20", "message.alert", "Revisa a tu cuy"), null);
        assertThat(settings.findAll())
                .containsExactlyInAnyOrderEntriesOf(Map.of("health.observedToAlertMinutes", "20",
                        "message.alert", "Revisa a tu cuy"));

        settings.replaceAll(Map.of("assistant.maxQuestions", "5"), null);
        assertThat(settings.findAll()).containsExactlyEntriesOf(Map.of("assistant.maxQuestions", "5"));

        settings.replaceAll(Map.of(), null);
        assertThat(settings.findAll()).isEmpty();
    }
}
