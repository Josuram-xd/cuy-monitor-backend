package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.model.settings.SettingKey;
import com.cuymonitor.backend.domain.model.settings.SettingView;
import com.cuymonitor.backend.domain.model.settings.Settings;
import com.cuymonitor.backend.domain.port.out.SettingRepository;
import com.cuymonitor.backend.domain.port.out.SettingsListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * "Reglas y mensajes": reads, changes and resets the settings, and hands the result to its listeners (the health
 * engine, the assistant) at once, so a change needs no restart. Who may call it is the admin proxy's job.
 */
public class SettingsService {

    private final Map<SettingKey, String> defaults;
    private final SettingRepository repository;
    private final List<SettingsListener> listeners;
    private volatile Settings current;

    public SettingsService(Map<SettingKey, String> defaults, SettingRepository repository,
                           List<SettingsListener> listeners) {
        this.defaults = Map.copyOf(defaults);
        this.repository = Objects.requireNonNull(repository, "repository");
        this.listeners = List.copyOf(listeners);
        this.current = Settings.resolve(this.defaults, Map.of());
    }

    /** At startup: what the admin changed before, from the database, on top of the defaults. */
    public void applyStored() {
        apply(Settings.resolve(defaults, repository.findAll()));
    }

    public List<SettingView> view() {
        Settings now = current;
        return Arrays.stream(SettingKey.values())
                .map(key -> new SettingView(key.key(), key.kind(), now.text(key), defaults.get(key), key.min(),
                        key.max()))
                .toList();
    }

    /**
     * Applies these changes on top of the current values. Every value is checked first: one bad value changes
     * nothing. A value equal to its default is not stored, so a later default still reaches it.
     */
    public List<SettingView> update(Map<String, String> changes, UUID changedBy) {
        Map<SettingKey, String> next = new HashMap<>(current.asMap());
        changes.forEach((key, value) -> {
            SettingKey setting = SettingKey.of(key)
                    .orElseThrow(() -> new IllegalArgumentException("unknown setting " + key));
            next.put(setting, setting.normalize(value));
        });
        Map<String, String> stored = new HashMap<>();
        next.forEach((key, value) -> {
            if (!value.equals(defaults.get(key))) {
                stored.put(key.key(), value);
            }
        });
        repository.replaceAll(stored, changedBy);
        apply(Settings.resolve(defaults, stored));
        return view();
    }

    public List<SettingView> reset(UUID changedBy) {
        repository.replaceAll(Map.of(), changedBy);
        apply(Settings.resolve(defaults, Map.of()));
        return view();
    }

    private void apply(Settings settings) {
        current = settings;
        listeners.forEach(listener -> listener.onSettings(settings));
    }
}
