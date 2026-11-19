package com.cuymonitor.backend.domain.port.out;

import com.cuymonitor.backend.domain.model.settings.Settings;

/** Whoever has to follow the settings: told once at startup and again after every change, with all of them. */
public interface SettingsListener {

    void onSettings(Settings settings);
}
