package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.settings.Settings;
import com.cuymonitor.backend.domain.port.in.ProcessEventUseCase;
import com.cuymonitor.backend.domain.port.out.SettingsListener;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * The only ProcessEventUseCase. The health engine (chain, states, thresholds, messages) is built from the settings,
 * and built again when an admin changes them: an event in flight finishes with the rules it started with, the next
 * one uses the new ones. The transaction is opened here, around the whole evaluation.
 */
public class ConfigurableEventProcessor implements ProcessEventUseCase, SettingsListener {

    private final Function<Settings, ProcessEventUseCase> engineFor;
    private final AtomicReference<ProcessEventUseCase> engine;

    public ConfigurableEventProcessor(Function<Settings, ProcessEventUseCase> engineFor, Settings initial) {
        this.engineFor = Objects.requireNonNull(engineFor, "engineFor");
        this.engine = new AtomicReference<>(engineFor.apply(initial));
    }

    @Override
    @Transactional
    public void process(HealthEvent event) {
        engine.get().process(event);
    }

    @Override
    public void onSettings(Settings settings) {
        engine.set(engineFor.apply(settings));
    }
}
