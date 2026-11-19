package com.cuymonitor.backend.application.admin;

import com.cuymonitor.backend.application.SettingsService;
import com.cuymonitor.backend.domain.model.settings.SettingView;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Command — ConcreteCommand: puts every setting back to its default. */
public final class ResetSettingsCommand implements AdminCommand<List<SettingView>> {

    private final SettingsService settings;
    private final UUID adminId;

    public ResetSettingsCommand(SettingsService settings, UUID adminId) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.adminId = adminId;
    }

    @Override
    public List<SettingView> execute() {
        return settings.reset(adminId);
    }

    @Override
    public String description() {
        return "reset every setting to its default";
    }
}
