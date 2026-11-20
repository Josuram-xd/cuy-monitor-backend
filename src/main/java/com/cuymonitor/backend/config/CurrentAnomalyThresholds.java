package com.cuymonitor.backend.config;

import com.cuymonitor.backend.domain.model.AnomalyThresholds;
import com.cuymonitor.backend.domain.model.settings.Settings;
import com.cuymonitor.backend.domain.port.out.SettingsListener;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * The anomaly thresholds an admin set last ("Reglas y mensajes"), for the readers outside the health engine: the
 * live symptoms of the monitoring panel and the note of an alert say what stands out with the same rules.
 */
public class CurrentAnomalyThresholds implements SettingsListener, Supplier<AnomalyThresholds> {

    private final AtomicReference<AnomalyThresholds> current = new AtomicReference<>(AnomalyThresholds.defaults());

    @Override
    public void onSettings(Settings settings) {
        current.set(HealthProperties.from(settings).anomalyThresholds());
    }

    @Override
    public AnomalyThresholds get() {
        return current.get();
    }
}
